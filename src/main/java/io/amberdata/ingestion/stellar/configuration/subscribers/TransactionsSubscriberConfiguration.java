package io.amberdata.ingestion.stellar.configuration.subscribers;

import io.amberdata.ingestion.core.client.BlockchainEntityWithState;
import io.amberdata.ingestion.core.client.IngestionApiClient;
import io.amberdata.ingestion.core.state.ResourceStateStorage;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.stellar.sdk.FormatException;
import org.stellar.sdk.responses.TransactionResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.ingestion.domain.Transaction;
import io.amberdata.ingestion.stellar.client.HorizonServer;
import io.amberdata.ingestion.stellar.configuration.history.HistoricalManager;
import io.amberdata.ingestion.stellar.configuration.properties.BatchSettings;
import io.amberdata.ingestion.stellar.mapper.ModelMapper;

import javax.annotation.PostConstruct;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Configuration
@ConditionalOnProperty(prefix = "stellar", name="subscribe-on-transactions")
public class TransactionsSubscriberConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionsSubscriberConfiguration.class);

    private final ResourceStateStorage        stateStorage;
    private final IngestionApiClient          apiClient;
    private final ModelMapper                 modelMapper;
    private final HistoricalManager           historicalManager;
    private final HorizonServer               server;
    private final BatchSettings               batchSettings;

    public TransactionsSubscriberConfiguration (ResourceStateStorage stateStorage,
                                                IngestionApiClient apiClient,
                                                ModelMapper modelMapper,
                                                HistoricalManager historicalManager,
                                                HorizonServer server,
                                                BatchSettings batchSettings) {
        this.stateStorage = stateStorage;
        this.apiClient = apiClient;
        this.modelMapper = modelMapper;
        this.historicalManager = historicalManager;
        this.server = server;
        this.batchSettings = batchSettings;
    }

    @PostConstruct
    public void createPipeline () {
        LOG.info("Going to subscribe on Stellar Transactions stream");

        Flux.<TransactionResponse>push(sink -> subscribe(sink::next))
            .publishOn(Schedulers.newElastic("transactions-subscriber-thread"))
            .doOnNext(tx -> LOG.info("Received transaction with hash {}", tx.getHash()))
            .map(this::enrichTransaction)
            .buffer(this.batchSettings.transactionsInChunk())
            .retryWhen(SubscriberErrorsHandler::onError)
            .subscribe(
                entities -> this.apiClient.publish("/transactions", entities),
                SubscriberErrorsHandler::handleFatalApplicationError
            );
    }

    private BlockchainEntityWithState<Transaction> enrichTransaction (TransactionResponse transactionResponse) {
        List<OperationResponse> operationResponses = fetchOperationsForTransaction(transactionResponse);
        return this.modelMapper.map(transactionResponse, operationResponses);
    }

    private List<OperationResponse> fetchOperationsForTransaction (TransactionResponse transactionResponse) {
        try {
            return this.server.horizonServer()
                .operations()
                .forTransaction(transactionResponse.getHash())
                .execute()
                .getRecords();
        }
        catch (IOException | FormatException ex) {
            LOG.error("Unable to fetch information about operations for transaction {}", transactionResponse.getHash());
            return Collections.emptyList();
        }
    }

    private void subscribe (Consumer<TransactionResponse> responseConsumer) {
        String cursorPointer = getCursorPointer();

        LOG.info("Transactions cursor is set to {}", cursorPointer);

        this.server.testConnection();
        testCursorCorrectness(cursorPointer);

        this.server.horizonServer()
            .transactions()
            .cursor(cursorPointer);
    }

    private String getCursorPointer () {
        if (this.historicalManager.disabled()) {
            return this.stateStorage.getStateToken(Transaction.class.getSimpleName(), () -> "now");
        } else {
            return this.historicalManager.transactionPagingToken();
        }
    }

    private void testCursorCorrectness (String cursorPointer) {
        try {
            this.server.horizonServer().transactions().cursor(cursorPointer).limit(1).execute();
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to test if cursor value is valid", e);
        }
    }
}
