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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import org.stellar.sdk.FormatException;
import org.stellar.sdk.responses.LedgerResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

@Configuration
@ConditionalOnProperty(prefix = "stellar", name = "subscribe-on-orders")
public class OrdersSubscriberConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(OrdersSubscriberConfiguration.class);

  private final ResourceStateStorage stateStorage;
  private final InboundApiClient apiClient;
  private final ModelMapper modelMapper;
  private final HistoricalManager historicalManager;
  private final HorizonServer server;
  private final BatchSettings batchSettings;
  private final SubscriberErrorsHandler errorsHandler;

  public OrdersSubscriberConfiguration(
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
    LOG.info("Going to subscribe on Stellar DEX Orders stream through Ledgers stream");

    Flux.<LedgerResponse>push(sink -> subscribe(sink::next))
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
        fetchOperationsForLedger(ledgerResponse),
        ledgerResponse.getSequence()
    ).stream()
        .map(
            order -> BlockchainEntityWithState.from(
              order,
              ResourceState.from(Order.class.getSimpleName(), ledgerResponse.getPagingToken())
            )
        );
  }

  private void subscribe(Consumer<LedgerResponse> stellarSdkResponseConsumer) {
    String cursorPointer = getCursorPointer();

    LOG.info("Subscribing to orders using ledger cursor {}", cursorPointer);

    this.server.testConnection();
    testCursorCorrectness(cursorPointer);

    this.server.horizonServer()
        .ledgers()
        .cursor(cursorPointer)
        .stream(stellarSdkResponseConsumer::accept);
  }

  private List<OperationResponse> fetchOperationsForLedger(LedgerResponse ledgerResponse) {
    try {
      return this.server.horizonServer()
          .operations()
          .forLedger(ledgerResponse.getSequence())
          .execute()
          .getRecords();
    } catch (IOException | FormatException ex) {
      LOG.error(
          "Unable to fetch information about operations for ledger " + ledgerResponse.getSequence(),
          ex
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
