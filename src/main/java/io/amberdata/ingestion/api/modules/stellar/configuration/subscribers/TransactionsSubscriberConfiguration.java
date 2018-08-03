package io.amberdata.ingestion.api.modules.stellar.configuration.subscribers;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.stellar.sdk.requests.TransactionsRequestBuilder;
import org.stellar.sdk.responses.TransactionResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.domain.Transaction;
import io.amberdata.ingestion.api.modules.stellar.client.HorizonServer;
import io.amberdata.ingestion.api.modules.stellar.client.IngestionApiClient;
import io.amberdata.ingestion.api.modules.stellar.mapper.ModelMapper;
import io.amberdata.ingestion.api.modules.stellar.state.ResourceStateStorage;
import io.amberdata.ingestion.api.modules.stellar.state.entities.Resource;

import javax.annotation.PostConstruct;
import reactor.core.publisher.Flux;

@Configuration
public class TransactionsSubscriberConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionsSubscriberConfiguration.class);

    private final ResourceStateStorage stateStorage;
    private final IngestionApiClient   apiClient;
    private final ModelMapper          modelMapper;
    private final HorizonServer        server;

    public TransactionsSubscriberConfiguration (ResourceStateStorage stateStorage,
                                                IngestionApiClient apiClient,
                                                ModelMapper modelMapper,
                                                HorizonServer server) {

        this.stateStorage = stateStorage;
        this.apiClient = apiClient;
        this.modelMapper = modelMapper;
        this.server = server;
    }

    @PostConstruct
    public void createPipeline () {
        Flux.<TransactionResponse>push(sink -> subscribe(sink::next))
            .retryWhen(SubscriberErrorsHandler::onError)
            .map(transactionResponse -> {
                List<OperationResponse> operationResponses = fetchOperationsForTransaction(transactionResponse);
                return modelMapper.map(transactionResponse, operationResponses);
            })
            .buffer(10)
            .map(mappedEntity -> apiClient.publish("/transactions", mappedEntity, Transaction.class))
            .subscribe(stateStorage::storeState, SubscriberErrorsHandler::handleFatalApplicationError);
    }

    private List<OperationResponse> fetchOperationsForTransaction (TransactionResponse transactionResponse) {
        try {
            return server.horizonServer()
                .operations()
                .forTransaction(transactionResponse.getHash())
                .execute()
                .getRecords();
        }
        catch (IOException ex) {
            return Collections.emptyList();
        }
    }

    private void subscribe (Consumer<TransactionResponse> responseConsumer) {
        String cursorPointer = stateStorage.getCursorPointer(Resource.TRANSACTION);

        LOG.info("Transactions cursor is set to {}", cursorPointer);

        TransactionsRequestBuilder requestBuilder = server.horizonServer()
            .transactions()
            .cursor(cursorPointer);

        server.testConnection();
        testCursorCorrectness(cursorPointer);

        requestBuilder.stream(responseConsumer::accept);
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