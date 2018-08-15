package io.amberdata.ingestion.stellar.configuration.subscribers;

import io.amberdata.ingestion.core.client.IngestionApiClient;
import io.amberdata.ingestion.core.state.ResourceStateStorage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.stellar.sdk.FormatException;
import org.stellar.sdk.responses.TransactionResponse;
import org.stellar.sdk.responses.effects.EffectResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.ingestion.domain.Transaction;
import io.amberdata.ingestion.stellar.client.HorizonServer;
import io.amberdata.ingestion.stellar.configuration.properties.BatchSettings;
import io.amberdata.ingestion.stellar.configuration.subscribers.SubscriberErrorsHandler;
import io.amberdata.ingestion.stellar.mapper.ModelMapper;
import io.amberdata.ingestion.stellar.util.PreAuthTransactionProcessor;

import javax.annotation.PostConstruct;
import reactor.core.publisher.Flux;

@Configuration
@ConditionalOnProperty(prefix = "stellar", name="subscribe-on-transactions")
public class TransactionsSubscriberConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionsSubscriberConfiguration.class);

    private final ResourceStateStorage        stateStorage;
    private final IngestionApiClient          apiClient;
    private final ModelMapper                 modelMapper;
    private final HorizonServer               server;
    private final BatchSettings               batchSettings;
    private final PreAuthTransactionProcessor preAuthTransactionProcessor;

    public TransactionsSubscriberConfiguration (ResourceStateStorage stateStorage,
                                                IngestionApiClient apiClient,
                                                ModelMapper modelMapper,
                                                HorizonServer server,
                                                BatchSettings batchSettings,
                                                PreAuthTransactionProcessor preAuthTransactionProcessor) {

        this.stateStorage = stateStorage;
        this.apiClient = apiClient;
        this.modelMapper = modelMapper;
        this.server = server;
        this.batchSettings = batchSettings;
        this.preAuthTransactionProcessor = preAuthTransactionProcessor;
    }

    @PostConstruct
    public void createPipeline () {
        LOG.info("Going to subscribe on Stellar Transactions stream");

        Flux.<TransactionResponse>push(sink -> subscribe(sink::next))
            .retryWhen(SubscriberErrorsHandler::onError)
            .doOnNext(tx -> LOG.info("Received transaction with hash {}", tx.getHash()))
            .map(transactionResponse -> {
                List<OperationResponse> operationResponses = fetchOperationsForTransaction(transactionResponse);
                return modelMapper.map(transactionResponse, operationResponses);
            })
            .buffer(Integer.parseInt(batchSettings.getTransactionsInChunk()))
            .map(mappedEntity -> apiClient.publish("/transactions", mappedEntity, Transaction.class))
            .subscribe(null, SubscriberErrorsHandler::handleFatalApplicationError);
    }

    private List<OperationResponse> fetchOperationsForTransaction (TransactionResponse transactionResponse) {
        try {
            return server.horizonServer()
                .operations()
                .forTransaction(transactionResponse.getHash())
                .execute()
                .getRecords();
        }
        catch (FormatException ex) {
            return this.preAuthTransactionProcessor.fetchOperations(transactionResponse.getHash());
        }
        catch (IOException ex) {
            LOG.error("Unable to fetch information about operations for transaction {}", transactionResponse.getHash());
            return Collections.emptyList();
        }
    }

    private void subscribe (Consumer<TransactionResponse> responseConsumer) {
        String cursorPointer = stateStorage.getStateToken(Transaction.class.getSimpleName(), () -> "now");

        LOG.info("Transactions cursor is set to {}", cursorPointer);

        server.testConnection();
        testCursorCorrectness(cursorPointer);

        server.horizonServer()
            .transactions()
            .cursor(cursorPointer)
            .stream(responseConsumer::accept);
    }

    private void testCursorCorrectness (String cursorPointer) {
        try {
            server.horizonServer().transactions().cursor(cursorPointer).limit(1).execute();
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to test if cursor value is valid", e);
        }
    }
}
