package io.amberdata.inbound.stellar.configuration.subscribers;

import io.amberdata.inbound.core.client.BlockchainEntityWithState;
import io.amberdata.inbound.core.client.InboundApiClient;
import io.amberdata.inbound.core.state.ResourceStateStorage;
import io.amberdata.inbound.domain.Transaction;
import io.amberdata.inbound.stellar.client.HorizonServer;
import io.amberdata.inbound.stellar.configuration.history.HistoricalManager;
import io.amberdata.inbound.stellar.configuration.properties.BatchSettings;
import io.amberdata.inbound.stellar.mapper.ModelMapper;

import java.io.IOException;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

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
@ConditionalOnProperty(prefix = "stellar", name = "subscribe-on-transactions")
public class TransactionsSubscriberConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(
      TransactionsSubscriberConfiguration.class
  );

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
  public TransactionsSubscriberConfiguration(
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
   * Creates the transactions pipeline.
   */
  @PostConstruct
  public void createPipeline() {
    LOG.info("Going to subscribe on Stellar Transactions stream");

    Flux
        .<TransactionResponse>push(
          sink -> subscribe(
            sink::next,
            SubscriberErrorsHandler::handleFatalApplicationError
          )
        )
        .publishOn(Schedulers.newElastic("transactions-subscriber-thread"))
        .timeout(this.errorsHandler.timeoutDuration())
        .map(this::enrichTransaction)
        .buffer(this.batchSettings.transactionsInChunk())
        .retryWhen(errorsHandler::onError)
        .subscribe(
          entities -> this.apiClient.publishWithState("/transactions", entities),
          SubscriberErrorsHandler::handleFatalApplicationError
      );
  }

  private BlockchainEntityWithState<Transaction> enrichTransaction(
      TransactionResponse transactionResponse
  ) {
    List<OperationResponse> operationResponses =
        this.fetchOperationsForTransaction(transactionResponse);
    return this.modelMapper.mapTransactionWithState(transactionResponse, operationResponses);
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
          "Unable to fetch information about operations for transaction: "
          + transactionResponse.getHash(),
          e
      );
      return Collections.emptyList();
    }
  }

  private void subscribe(Consumer<TransactionResponse> responseConsumer,
                         Consumer<? super Throwable>   errorConsumer) {
    String cursorPointer = this.getCursorPointer();

    LOG.info("Subscribing to transactions using cursor {}", cursorPointer);

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
      return this.stateStorage.getStateToken(Transaction.class.getSimpleName(), () -> "now");
    } else {
      return this.historicalManager.transactionPagingToken();
    }
  }

  private void testCursorCorrectness(String cursorPointer) {
    try {
      this.server.horizonServer().transactions().cursor(cursorPointer).limit(1).execute();
    } catch (IOException ioe) {
      throw new RuntimeException("Failed to test if cursor value is valid", ioe);
    }
  }

}
