package io.amberdata.inbound.stellar.configuration.subscribers;

import io.amberdata.inbound.core.client.BlockchainEntityWithState;
import io.amberdata.inbound.core.client.InboundApiClient;
import io.amberdata.inbound.core.state.ResourceStateStorage;
import io.amberdata.inbound.core.state.entities.ResourceState;
import io.amberdata.inbound.domain.Order;
import io.amberdata.inbound.stellar.client.HorizonServer;
import io.amberdata.inbound.stellar.configuration.history.HistoricalManager;
import io.amberdata.inbound.stellar.configuration.properties.BatchSettings;
import io.amberdata.inbound.stellar.mapper.ModelMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import org.stellar.sdk.FormatException;
import org.stellar.sdk.requests.EventListener;
import org.stellar.sdk.responses.LedgerResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import shadow.com.google.common.base.Optional;

@Configuration
@ConditionalOnProperty(prefix = "stellar", name = "subscribe-on-orders")
public class OrdersSubscriberConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(OrdersSubscriberConfiguration.class);

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
  public OrdersSubscriberConfiguration(
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
   * Creates the order pipeline.
   */
  @PostConstruct
  public void createPipeline() {
    LOG.info("Going to subscribe on Stellar DEX Orders stream through Ledgers stream");

    Flux
        .<LedgerResponse>push(
          sink -> subscribe(
            sink::next,
            SubscriberErrorsHandler::handleFatalApplicationError
          )
        )
        .publishOn(Schedulers.newElastic("orders-subscriber-thread"))
        .timeout(this.errorsHandler.timeoutDuration())
        .map(this::toOrdersStream)
        .flatMap(Flux::fromStream)
        .buffer(this.batchSettings.ordersInChunk())
        .retryWhen(errorsHandler::onError)
        .subscribe(
          entities -> this.apiClient.publishWithState("/orders", entities),
          SubscriberErrorsHandler::handleFatalApplicationError
      );
  }

  private Stream<BlockchainEntityWithState<Order>> toOrdersStream(LedgerResponse ledgerResponse) {
    return this.modelMapper.mapOrders(
      this.fetchOperationsForLedger(ledgerResponse),
      ledgerResponse.getSequence()
    )
      .stream()
      .map(
        order -> BlockchainEntityWithState.from(
          order,
          ResourceState.from(Order.class.getSimpleName(), ledgerResponse.getPagingToken())
        )
      );
  }

  private void subscribe(Consumer<LedgerResponse>    responseConsumer,
                         Consumer<? super Throwable> errorConsumer) {
    String cursorPointer = this.getCursorPointer();

    LOG.info("Subscribing to orders using ledger cursor {}", cursorPointer);

    this.server.testConnection();
    this.testCursorCorrectness(cursorPointer);

    this.server.horizonServer()
        .ledgers()
        .cursor(cursorPointer)
        .limit(HorizonServer.HORIZON_PER_REQUEST_LIMIT)
        .stream(new EventListener<LedgerResponse>() {
          @Override
          public void onEvent(LedgerResponse ledgerResponse) {
            responseConsumer.accept(ledgerResponse);
          }

          @Override
          public void onFailure(Optional<Throwable> optional, Optional<Integer> optional1) {
            if (optional.isPresent()) {
              errorConsumer.accept(optional.get());
            }
          }
        });
  }

  private List<OperationResponse> fetchOperationsForLedger(LedgerResponse ledgerResponse) {
    try {
      return StellarSubscriberConfiguration.getObjects(
        this.server,
        this.server.horizonServer()
          .operations()
          .forLedger(ledgerResponse.getSequence())
          .limit(HorizonServer.HORIZON_PER_REQUEST_LIMIT)
          .execute(),
        "ledger.operations"
      );
    } catch (IOException | FormatException e) {
      LOG.error(
          "Unable to fetch information about operations for ledger " + ledgerResponse.getSequence(),
          e
      );
      return Collections.emptyList();
    }
  }

  private String getCursorPointer() {
    if (this.historicalManager.disabled()) {
      return this.stateStorage.getStateToken(Order.class.getSimpleName(), () -> "now");
    } else {
      return this.historicalManager.ledgerPagingToken();
    }
  }

  private void testCursorCorrectness(String cursorPointer) {
    try {
      this.server.horizonServer().ledgers().cursor(cursorPointer).limit(1).execute();
    } catch (IOException ioe) {
      throw new HorizonServer.IncorrectRequestException(
          "Failed to test if cursor value is valid",
          ioe
      );
    }
  }

}
