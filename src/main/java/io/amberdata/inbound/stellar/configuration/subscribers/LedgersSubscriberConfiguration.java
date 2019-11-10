package io.amberdata.inbound.stellar.configuration.subscribers;

import io.amberdata.inbound.core.client.InboundApiClient;
import io.amberdata.inbound.core.state.ResourceStateStorage;
import io.amberdata.inbound.domain.Block;
import io.amberdata.inbound.stellar.client.HorizonServer;
import io.amberdata.inbound.stellar.configuration.history.HistoricalManager;
import io.amberdata.inbound.stellar.configuration.properties.BatchSettings;
import io.amberdata.inbound.stellar.mapper.ModelMapper;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import org.stellar.sdk.responses.LedgerResponse;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Configuration
@ConditionalOnProperty(prefix = "stellar", name = "subscribe-on-ledgers")
public class LedgersSubscriberConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(LedgersSubscriberConfiguration.class);

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
  public LedgersSubscriberConfiguration(
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
   * Creates the ledger pipeline.
   */
  @PostConstruct
  public void createPipeline() {
    LOG.info("Going to subscribe on Stellar Ledgers stream");

    Flux
        .<LedgerResponse>push(
          sink -> StellarSubscriberConfiguration.subscribeToLedgers(
            this.server,
            this.getCursorPointer(),
            sink::next,
            SubscriberErrorsHandler::handleFatalApplicationError
          )
        )
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

  private String getCursorPointer() {
    if (this.historicalManager.disabled()) {
      return this.stateStorage.getStateToken(Block.class.getSimpleName(), () -> "now");
    } else {
      return this.historicalManager.ledgerPagingToken();
    }
  }

}
