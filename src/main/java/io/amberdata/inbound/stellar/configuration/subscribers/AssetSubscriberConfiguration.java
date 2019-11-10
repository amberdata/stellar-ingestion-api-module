package io.amberdata.inbound.stellar.configuration.subscribers;

import io.amberdata.inbound.core.client.BlockchainEntityWithState;
import io.amberdata.inbound.core.client.InboundApiClient;
import io.amberdata.inbound.core.state.ResourceStateStorage;
import io.amberdata.inbound.core.state.entities.ResourceState;
import io.amberdata.inbound.domain.Asset;
import io.amberdata.inbound.stellar.client.HorizonServer;
import io.amberdata.inbound.stellar.configuration.history.HistoricalManager;
import io.amberdata.inbound.stellar.configuration.properties.BatchSettings;
import io.amberdata.inbound.stellar.mapper.ModelMapper;

import java.io.IOException;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import org.stellar.sdk.FormatException;
import org.stellar.sdk.requests.EventListener;
import org.stellar.sdk.responses.TransactionResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import shadow.com.google.common.base.Optional;

@Configuration
@ConditionalOnProperty(prefix = "stellar", name = "subscribe-on-assets")
public class AssetSubscriberConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(AssetSubscriberConfiguration.class);

  private final ResourceStateStorage    stateStorage;
  private final InboundApiClient        apiClient;
  private final ModelMapper             modelMapper;
  private final HistoricalManager       historicalManager;
  private final HorizonServer           server;
  private final BatchSettings           batchSettings;
  private final SubscriberErrorsHandler errorsHandler;

  /**
   * Default constrcutor.
   *
   * @param stateStorage      the state storage
   * @param apiClient         the client api
   * @param modelMapper       the model mapper
   * @param historicalManager the historical manager
   * @param server            the Horizon server
   * @param batchSettings     the batch settings
   * @param errorsHandler     the error handler
   */
  public AssetSubscriberConfiguration(
      ResourceStateStorage    stateStorage,
      InboundApiClient        apiClient,
      ModelMapper             modelMapper,
      HistoricalManager       historicalManager,
      HorizonServer           server,
      BatchSettings           batchSettings,
      SubscriberErrorsHandler errorsHandler
  ) {
    this.stateStorage      = stateStorage;
    this.apiClient         = apiClient;
    this.modelMapper       = modelMapper;
    this.historicalManager = historicalManager;
    this.server            = server;
    this.batchSettings     = batchSettings;
    this.errorsHandler     = errorsHandler;
  }

  /**
   * Creates the asset pipeline.
   */
  @PostConstruct
  public void createPipeline() {
    LOG.info("Going to subscribe on Stellar Assets stream through Transactions stream");

    Flux
        .<TransactionResponse>create(
          sink -> subscribe(
            sink::next,
            SubscriberErrorsHandler::handleFatalApplicationError)
        )
        .publishOn(Schedulers.newElastic("assets-subscriber-thread"))
        .timeout(this.errorsHandler.timeoutDuration())
        .map(this::toAssetsStream)
        .flatMap(Flux::fromStream)
        .buffer(this.batchSettings.assetsInChunk())
        .retryWhen(errorsHandler::onError)
        .subscribe(
          entities -> this.apiClient.publishWithState("/assets", entities),
          SubscriberErrorsHandler::handleFatalApplicationError
      );
  }

  private Stream<BlockchainEntityWithState<Asset>> toAssetsStream(
      TransactionResponse transactionResponse
  ) {
    return this.processAssets(
      this.fetchOperationsForTransaction(transactionResponse),
      transactionResponse.getLedger()
    )
      .stream()
      .map(
        asset -> BlockchainEntityWithState.from(
          asset,
          ResourceState.from(Asset.class.getSimpleName(), transactionResponse.getPagingToken())
        )
      );
  }

  private List<Asset> processAssets(List<OperationResponse> operationResponses, Long ledger) {
    return this.modelMapper.mapAssets(operationResponses, ledger)
      .stream()
      .distinct()
      .map(asset -> StellarSubscriberConfiguration.enrichAsset(this.server, asset))
      .collect(Collectors.toList());
  }

  private List<OperationResponse> fetchOperationsForTransaction(
      TransactionResponse transactionResponse
  ) {
    try {
      return StellarSubscriberConfiguration.getObjects(
        this.server,
        this.server.horizonServer()
          .operations()
          .forTransaction(transactionResponse.getHash())
          .limit(StellarSubscriberConfiguration.DEFAULT_LIMIT)
          .execute()
      );
    } catch (IOException | FormatException e) {
      LOG.error(
          "Unable to fetch information about operations for transaction "
          + transactionResponse.getHash(),
          e
      );
      return Collections.emptyList();
    }
  }

  private void subscribe(Consumer<TransactionResponse> responseConsumer,
                         Consumer<? super Throwable>   errorConsumer) {
    String cursorPointer = getCursorPointer();

    LOG.info("Subscribing to assets using transactions cursor {}", cursorPointer);

    this.server.testConnection();
    this.testCursorCorrectness(cursorPointer);

    this.server.horizonServer()
        .transactions()
        .cursor(cursorPointer)
        .stream(new EventListener<TransactionResponse>() {
          @Override
          public void onEvent(TransactionResponse transactionResponse) {
            responseConsumer.accept(transactionResponse);
          }

          @Override
          public void onFailure(Optional<Throwable> optional, Optional<Integer> optional1) {
            if (optional.isPresent()) {
              errorConsumer.accept(optional.get());
            }
          }
        });
  }

  private String getCursorPointer() {
    if (this.historicalManager.disabled()) {
      return this.stateStorage.getStateToken(Asset.class.getSimpleName(), () -> "now");
    } else {
      return this.historicalManager.transactionPagingToken();
    }
  }

  private void testCursorCorrectness(String cursorPointer) {
    try {
      this.server.horizonServer().transactions().cursor(cursorPointer).limit(1).execute();
    } catch (IOException ioe) {
      throw new HorizonServer.IncorrectRequestException(
          "Failed to test if cursor value is valid",
          ioe
      );
    }
  }

}
