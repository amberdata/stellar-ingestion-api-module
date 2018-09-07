package io.amberdata.inbound.stellar.configuration.subscribers;

import io.amberdata.inbound.core.client.BlockchainEntityWithState;
import io.amberdata.inbound.core.client.InboundApiClient;
import io.amberdata.inbound.core.state.ResourceStateStorage;
import io.amberdata.inbound.domain.Transaction;
import io.amberdata.inbound.stellar.client.HorizonServer;
import io.amberdata.inbound.stellar.configuration.history.HistoricalManager;
import io.amberdata.inbound.stellar.configuration.properties.BatchSettings;
import io.amberdata.inbound.stellar.mapper.ModelMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import org.stellar.sdk.FormatException;
import org.stellar.sdk.responses.TransactionResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;

@Configuration
@ConditionalOnProperty(prefix = "stellar", name = "subscribe-on-transactions")
public class TransactionsSubscriberConfiguration {

  private static final Logger LOG = LoggerFactory
      .getLogger(TransactionsSubscriberConfiguration.class);

  private final ResourceStateStorage stateStorage;
  private final InboundApiClient apiClient;
  private final ModelMapper modelMapper;
  private final HistoricalManager historicalManager;
  private final HorizonServer server;
  private final BatchSettings batchSettings;
  private final SubscriberErrorsHandler errorsHandler;

  public TransactionsSubscriberConfiguration(
      ResourceStateStorage stateStorage,
      InboundApiClient apiClient,
      ModelMapper modelMapper,
      HistoricalManager historicalManager,
      HorizonServer server,
      BatchSettings batchSettings,
      SubscriberErrorsHandler errorsHandler
  ) {
    this.stateStorage = stateStorage;
    this.apiClient = apiClient;
    this.modelMapper = modelMapper;
    this.historicalManager = historicalManager;
    this.server = server;
    this.batchSettings = batchSettings;
    this.errorsHandler = errorsHandler;
  }

  @PostConstruct
  public void createPipeline() {
    LOG.info("Going to subscribe on Stellar Transactions stream");

    Flux.<TransactionResponse>push(sink -> subscribe(sink::next))
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
    List<OperationResponse> operationResponses = fetchOperationsForTransaction(transactionResponse);
    return this.modelMapper.mapTransactionWithState(transactionResponse, operationResponses);
  }

  private List<OperationResponse> fetchOperationsForTransaction(
      TransactionResponse transactionResponse
  ) {
    try {
      return this.server.horizonServer()
          .operations()
          .forTransaction(transactionResponse.getHash())
          .execute()
          .getRecords();
    } catch (IOException | FormatException ex) {
      LOG.error("Unable to fetch information about operations for transaction {}",
          transactionResponse.getHash());
      return Collections.emptyList();
    }
  }

  private void subscribe(Consumer<TransactionResponse> responseConsumer) {
    String cursorPointer = getCursorPointer();

    LOG.info("Subscribing to transactions using cursor {}", cursorPointer);

    this.server.testConnection();
    testCursorCorrectness(cursorPointer);

    this.server.horizonServer()
        .transactions()
        .cursor(cursorPointer)
        .stream(responseConsumer::accept);
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