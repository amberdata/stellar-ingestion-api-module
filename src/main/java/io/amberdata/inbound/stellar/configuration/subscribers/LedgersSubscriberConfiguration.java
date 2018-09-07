package io.amberdata.inbound.stellar.configuration.subscribers;

import io.amberdata.inbound.core.client.InboundApiClient;
import io.amberdata.inbound.core.state.ResourceStateStorage;
import io.amberdata.inbound.domain.Block;
import io.amberdata.inbound.stellar.client.HorizonServer;
import io.amberdata.inbound.stellar.configuration.history.HistoricalManager;
import io.amberdata.inbound.stellar.configuration.properties.BatchSettings;
import io.amberdata.inbound.stellar.mapper.ModelMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import org.stellar.sdk.responses.LedgerResponse;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;

import java.util.function.Consumer;

import javax.annotation.PostConstruct;

@Configuration
@ConditionalOnProperty(prefix = "stellar", name = "subscribe-on-ledgers")
public class LedgersSubscriberConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(LedgersSubscriberConfiguration.class);

  private final ResourceStateStorage stateStorage;
  private final InboundApiClient apiClient;
  private final ModelMapper modelMapper;
  private final HistoricalManager historicalManager;
  private final HorizonServer server;
  private final BatchSettings batchSettings;
  private final SubscriberErrorsHandler errorsHandler;

  public LedgersSubscriberConfiguration(
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
    LOG.info("Going to subscribe on Stellar Ledgers stream");

    Flux.<LedgerResponse>push(sink -> subscribe(sink::next))
        .publishOn(Schedulers.newElastic("ledgers-subscriber-thread"))
        .timeout(this.errorsHandler.timeoutDuration())
        .map(this.modelMapper::mapLedgerWithState)
        .buffer(this.batchSettings.blocksInChunk())
        .retryWhen(errorsHandler::onError)
        .subscribe(
            entities -> this.apiClient.publishWithState("/blocks", entities),
            SubscriberErrorsHandler::handleFatalApplicationError
        );
  }

  private void subscribe(Consumer<LedgerResponse> stellarSdkResponseConsumer) {
    String cursorPointer = getCursorPointer();

    LOG.info("Subscribing to ledgers using cursor {}", cursorPointer);

    this.server.testConnection();
    testCursorCorrectness(cursorPointer);

    this.server.horizonServer()
        .ledgers()
        .cursor(cursorPointer)
        .stream(stellarSdkResponseConsumer::accept);
  }

  private String getCursorPointer() {
    if (this.historicalManager.disabled()) {
      return this.stateStorage.getStateToken(Block.class.getSimpleName(), () -> "now");
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
