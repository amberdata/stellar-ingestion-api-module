package io.amberdata.inbound.stellar.configuration.subscribers;

import io.amberdata.inbound.core.client.BlockchainEntityWithState;
import io.amberdata.inbound.core.client.InboundApiClient;
import io.amberdata.inbound.core.state.ResourceStateStorage;
import io.amberdata.inbound.core.state.entities.ResourceState;
import io.amberdata.inbound.domain.Trade;
import io.amberdata.inbound.stellar.client.HorizonServer;
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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import org.stellar.sdk.requests.EventListener;
import org.stellar.sdk.requests.RequestBuilder;
import org.stellar.sdk.requests.TradesRequestBuilder;
import org.stellar.sdk.responses.LedgerResponse;
import org.stellar.sdk.responses.TradeResponse;
import org.stellar.sdk.responses.TransactionResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import shadow.com.google.common.base.Optional;
import shadow.okhttp3.HttpUrl;

@Configuration
@ConditionalOnProperty(prefix = "stellar", name = "subscribe-on-trades")
public class TradesSubscriberConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(TradesSubscriberConfiguration.class);
  private static final String NOW_CURSOR_POINTER = "now";

  @Value("${stellar.trades.limit-for-one-ledger}")
  private int tradesLimit;

  @Value("${stellar.trades.upload-history}")
  private boolean uploadHistory;

  private String currentCursor;

  private final ResourceStateStorage    stateStorage;
  private final InboundApiClient        apiClient;
  private final ModelMapper             modelMapper;
  private final HorizonServer           server;
  private final BatchSettings           batchSettings;
  private final SubscriberErrorsHandler errorsHandler;

  /**
   * Default constrcutor.
   *
   * @param stateStorage      the state storage
   * @param apiClient         the client api
   * @param modelMapper       the model mapper
   * @param server            the Horizon server
   * @param batchSettings     the batch settings
   * @param errorsHandler     the error handler
   */
  public TradesSubscriberConfiguration(
      ResourceStateStorage    stateStorage,
      InboundApiClient        apiClient,
      ModelMapper             modelMapper,
      HorizonServer           server,
      BatchSettings           batchSettings,
      SubscriberErrorsHandler errorsHandler
  ) {
    this.stateStorage  = stateStorage;
    this.apiClient     = apiClient;
    this.modelMapper   = modelMapper;
    this.server        = server;
    this.batchSettings = batchSettings;
    this.errorsHandler = errorsHandler;
  }

  /**
   * Creates the trade pipeline.
   */
  @PostConstruct
  public void createPipeline() {
    LOG.info("Going to subscribe on Stellar DEX Trades stream through Ledgers stream");

    Flux
        .<LedgerResponse>push(
          sink -> subscribe(
            sink::next,
            SubscriberErrorsHandler::handleFatalApplicationError
          )
        )
        .publishOn(Schedulers.newElastic("trades-subscriber-thread"))
        .timeout(this.errorsHandler.timeoutDuration())
        .map(e -> toTradesStream())
        .flatMap(Flux::fromStream)
        .buffer(this.batchSettings.tradesInChunk())
        .retryWhen(errorsHandler::onError)
        .subscribe(
          entities -> this.apiClient.publishWithState("/trades", entities),
          SubscriberErrorsHandler::handleFatalApplicationError
      );
  }

  private Stream<BlockchainEntityWithState<Trade>> toTradesStream() {
    return this.fetchTrades()
      .stream()
      .map(
        trade -> BlockchainEntityWithState.from(
          trade,
          ResourceState.from(Trade.class.getSimpleName(), this.currentCursor)
        )
      );
  }

  private void subscribe(Consumer<LedgerResponse>    responseConsumer,
                         Consumer<? super Throwable> errorConsumer) {
    LOG.info("Subscribing to trades using ledger cursor {}", NOW_CURSOR_POINTER);

    this.server.testConnection();
    this.testCursorCorrectness(NOW_CURSOR_POINTER);

    this.server.horizonServer()
        .ledgers()
        .cursor(NOW_CURSOR_POINTER)
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

  private List<Trade> fetchTrades() {
    if (currentCursor == null) {
      this.currentCursor = this.getCursorPointer();
    }

    this.server.testConnection();
    this.testTradesCursorCorrectness(currentCursor);

    TradesRequestBuilder requestBuilder = this.server.horizonServer()
        .trades()
        .cursor(this.currentCursor)
        .limit(this.tradesLimit);

    try {
      List<TradeResponse> records = StellarSubscriberConfiguration.getObjects(
          this.server,
          requestBuilder.execute(),
          "trades"
      );
      if (records.isEmpty()) {
        return Collections.emptyList();
      }

      List<ExtendedTradeResponse> enrichedRecords = this.enrichRecords(records);

      this.currentCursor = records.get(records.size() - 1).getPagingToken();
      return this.modelMapper.mapTrades(enrichedRecords);
    } catch (IOException ioe) {
      throw new HorizonServer.IncorrectRequestException("Failed to get trades", ioe);
    }
  }

  private List<ExtendedTradeResponse> enrichRecords(List<TradeResponse> records) {
    return records
      .stream()
      .map(this::enrichRecord)
      .collect(Collectors.toList());
  }

  private ExtendedTradeResponse enrichRecord(TradeResponse tradeResponse) {
    Long   ledger          = 0L;
    String transactionHash = "";
    String operationHash   = "";
    try {
      HttpUrl httpUrl = HttpUrl.get(tradeResponse.getLinks().getOperation().getUri());

      OperationResponse operationResponse = this.server.horizonServer()
          .operations()
          .operation(httpUrl);

      transactionHash = operationResponse.getTransactionHash();
      operationHash   = operationResponse.getId().toString();

      TransactionResponse transactionResponse = this.server.horizonServer()
          .transactions()
          .transaction(transactionHash);

      ledger = transactionResponse.getLedger();
    } catch (Exception e) {
      LOG.error("Failed to get additional information for trade: " + tradeResponse.getId(), e);
    }

    return ExtendedTradeResponse.from(tradeResponse, ledger, transactionHash, operationHash);
  }

  private String getCursorPointer() {
    if (!this.uploadHistory) {
      return this.stateStorage.getStateToken(Trade.class.getSimpleName(), this::fetchCurrentCursor);
    } else {
      return this.fetchFirstCursor();
    }
  }

  private String fetchFirstCursor() {
    TradesRequestBuilder requestBuilder = this.server.horizonServer().trades().limit(1);

    try {
      List<TradeResponse> trades = requestBuilder.execute().getRecords();
      if (trades.isEmpty()) {
        throw new HorizonServer.IncorrectRequestException(
            "Failed to get initial cursor pointer for trades"
        );
      }

      return trades.get(0).getPagingToken();
    } catch (IOException ioe) {
      throw new HorizonServer.IncorrectRequestException(
          "Failed to get initial cursor pointer for trades",
          ioe
      );
    }
  }

  private String fetchCurrentCursor() {
    TradesRequestBuilder requestBuilder = (TradesRequestBuilder) this.server.horizonServer()
        .trades()
        .cursor(NOW_CURSOR_POINTER)
        .order(RequestBuilder.Order.DESC)
        .limit(1);

    try {
      List<TradeResponse> trades = requestBuilder.execute().getRecords();
      if (trades.isEmpty()) {
        throw new HorizonServer.IncorrectRequestException(
            "Failed to get current cursor pointer for trades"
        );
      }

      return trades.get(0).getPagingToken();
    } catch (IOException ioe) {
      throw new HorizonServer.IncorrectRequestException(
          "Failed to get current cursor pointer for trades",
          ioe
      );
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

  private void testTradesCursorCorrectness(String cursorPointer) {
    try {
      (this.server.horizonServer().trades().cursor(cursorPointer).limit(1)).execute();
    } catch (IOException ioe) {
      throw new HorizonServer.IncorrectRequestException(
          "Failed to test if cursor value is valid",
          ioe
      );
    }
  }

}
