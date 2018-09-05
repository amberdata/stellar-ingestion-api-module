package io.amberdata.inbound.stellar.configuration.subscribers;

import okhttp3.HttpUrl;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.stellar.sdk.requests.RequestBuilder;
import org.stellar.sdk.requests.TradesRequestBuilder;
import org.stellar.sdk.responses.LedgerResponse;
import org.stellar.sdk.responses.TradeResponse;
import org.stellar.sdk.responses.TransactionResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.inbound.core.client.BlockchainEntityWithState;
import io.amberdata.inbound.core.client.InboundApiClient;
import io.amberdata.inbound.core.state.ResourceStateStorage;
import io.amberdata.inbound.core.state.entities.ResourceState;
import io.amberdata.inbound.domain.Trade;
import io.amberdata.inbound.stellar.client.HorizonServer;
import io.amberdata.inbound.stellar.configuration.properties.BatchSettings;
import io.amberdata.inbound.stellar.mapper.ModelMapper;

@Configuration
@ConditionalOnProperty(prefix = "stellar", name = "subscribe-on-trades")
public class TradesSubscriberConfiguration {

    private static final Logger LOG                = LoggerFactory.getLogger(TradesSubscriberConfiguration.class);
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

    public TradesSubscriberConfiguration (ResourceStateStorage stateStorage,
                                          InboundApiClient apiClient,
                                          ModelMapper modelMapper,
                                          HorizonServer server,
                                          BatchSettings batchSettings,
                                          SubscriberErrorsHandler errorsHandler) {
        this.stateStorage = stateStorage;
        this.apiClient = apiClient;
        this.modelMapper = modelMapper;
        this.server = server;
        this.batchSettings = batchSettings;
        this.errorsHandler = errorsHandler;
    }

    @PostConstruct
    public void createPipeline () {
        LOG.info("Going to subscribe on Stellar DEX Trades stream through Ledgers stream");

        Flux.<LedgerResponse>push(sink -> subscribe(sink::next))
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

    private Stream<BlockchainEntityWithState<Trade>> toTradesStream () {
        return this.fetchTrades().stream()
            .map(trade -> BlockchainEntityWithState.from(
                trade,
                ResourceState.from(Trade.class.getSimpleName(), this.currentCursor)
            ));
    }

    private void subscribe (Consumer<LedgerResponse> stellarSdkResponseConsumer) {
        LOG.info("Subscribing to trades using ledger cursor {}", NOW_CURSOR_POINTER);

        this.server.testConnection();
        testCursorCorrectness(NOW_CURSOR_POINTER);

        this.server.horizonServer()
            .ledgers()
            .cursor(NOW_CURSOR_POINTER)
            .stream(stellarSdkResponseConsumer::accept);
    }

    private List<Trade> fetchTrades () {
        if (currentCursor == null) {
            this.currentCursor = getCursorPointer();
        }

        this.server.testConnection();
        testTradesCursorCorrectness(currentCursor);

        TradesRequestBuilder requestBuilder = (TradesRequestBuilder) this.server.horizonServer()
            .trades()
            .cursor(this.currentCursor)
            .limit(this.tradesLimit);

        try {
            List<TradeResponse> records = requestBuilder.execute().getRecords();
            if (records.size() == 0) {
                return Collections.emptyList();
            }

            List<ExtendedTradeResponse> enrichedRecords = enrichRecords(records);

            this.currentCursor = records.get(records.size() - 1).getPagingToken();
            return this.modelMapper.mapTrades(enrichedRecords);
        }
        catch (IOException e) {
            throw new HorizonServer.IncorrectRequestException("Failed to get trades", e);
        }
    }

    private List<ExtendedTradeResponse> enrichRecords (List<TradeResponse> records) {
        return records.stream()
            .map(this::enrichRecord)
            .collect(Collectors.toList());
    }

    private ExtendedTradeResponse enrichRecord (TradeResponse tradeResponse) {
        Long ledger = 0L;
        String transactionHash = "";
        String operationHash = "";
        try {
            HttpUrl httpUrl = HttpUrl.get(tradeResponse.getLinks().getOperation().getUri());

            OperationResponse operationResponse = this.server.horizonServer()
                .operations()
                .operation(httpUrl);

            transactionHash = operationResponse.getTransactionHash();
            operationHash = operationResponse.getId().toString();

            TransactionResponse transactionResponse = this.server.horizonServer()
                .transactions()
                .transaction(transactionHash);

            ledger = transactionResponse.getLedger();
        }
        catch (Exception e) {
            LOG.error("Failed to get additional information for trade: {}", tradeResponse.getId());
        }

        return ExtendedTradeResponse.from(tradeResponse, ledger, transactionHash, operationHash);
    }

    private String getCursorPointer () {
        if (!this.uploadHistory) {
            return this.stateStorage.getStateToken(Trade.class.getSimpleName(), this::fetchCurrentCursor);
        }
        else {
            return this.fetchFirstCursor();
        }
    }

    private String fetchFirstCursor () {
        TradesRequestBuilder requestBuilder = (TradesRequestBuilder) this.server.horizonServer()
            .trades()
            .limit(1);

        try {
            List<TradeResponse> trades = requestBuilder.execute().getRecords();
            if (trades.size() == 0) {
                throw new HorizonServer.IncorrectRequestException("Failed to get initial cursor pointer for trades");
            }

            return trades.get(0).getPagingToken();
        }
        catch (IOException e) {
            throw new HorizonServer.IncorrectRequestException("Failed to get initial cursor pointer for trades", e);
        }
    }

    private String fetchCurrentCursor () {
        TradesRequestBuilder requestBuilder = (TradesRequestBuilder) this.server.horizonServer()
            .trades()
            .cursor(NOW_CURSOR_POINTER)
            .order(RequestBuilder.Order.DESC)
            .limit(1);

        try {
            List<TradeResponse> trades = requestBuilder.execute().getRecords();
            if (trades.size() == 0) {
                throw new HorizonServer.IncorrectRequestException("Failed to get current cursor pointer for trades");
            }

            return trades.get(0).getPagingToken();
        }
        catch (IOException e) {
            throw new HorizonServer.IncorrectRequestException("Failed to get current cursor pointer for trades", e);
        }
    }

    private void testCursorCorrectness (String cursorPointer) {
        try {
            this.server.horizonServer().ledgers().cursor(cursorPointer).limit(1).execute();
        }
        catch (IOException e) {
            throw new HorizonServer.IncorrectRequestException("Failed to test if cursor value is valid", e);
        }
    }

    private void testTradesCursorCorrectness (String cursorPointer) {
        try {
            ((TradesRequestBuilder) this.server.horizonServer().trades().cursor(cursorPointer).limit(1)).execute();
        }
        catch (IOException e) {
            throw new HorizonServer.IncorrectRequestException("Failed to test if cursor value is valid", e);
        }
    }
}
