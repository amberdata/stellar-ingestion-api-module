package io.amberdata.ingestion.api.modules.stellar.configuration;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.stellar.sdk.Server;
import org.stellar.sdk.requests.TransactionsRequestBuilder;
import org.stellar.sdk.responses.TransactionResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.domain.Transaction;
import io.amberdata.ingestion.api.modules.stellar.StellarIngestionModuleDemoApplication;
import io.amberdata.ingestion.api.modules.stellar.client.IngestionApiClient;
import io.amberdata.ingestion.api.modules.stellar.mapper.ModelMapper;
import io.amberdata.ingestion.api.modules.stellar.state.BlockchainEntityWithState;
import io.amberdata.ingestion.api.modules.stellar.state.Resource;
import io.amberdata.ingestion.api.modules.stellar.state.ResourceState;
import io.amberdata.ingestion.api.modules.stellar.state.ResourceStateRepository;

import javax.annotation.PostConstruct;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Configuration
public class TransactionListenerConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionListenerConfiguration.class);

    private final ResourceStateRepository resourceStateRepository;
    private final IngestionApiClient      apiClient;
    private final ModelMapper             modelMapper;
    private final Server                  horizonServer;

    public TransactionListenerConfiguration (ResourceStateRepository resourceStateRepository,
                                             IngestionApiClient apiClient,
                                             ModelMapper modelMapper,
                                             Server horizonServer) {

        this.resourceStateRepository = resourceStateRepository;
        this.apiClient = apiClient;
        this.modelMapper = modelMapper;
        this.horizonServer = horizonServer;
    }

    @PostConstruct
    public void createPipeline () {
        Flux.<TransactionResponse>push(sink -> subscribe(sink::next))
            .retryWhen(companion -> companion
                .doOnNext(throwable -> LOG.error("Error occurred: {}", throwable))
                .zipWith(Flux.range(1, Integer.MAX_VALUE), this::retryCountPattern)
                .flatMap(this::retryBackOffPattern)
            )
            .map(transactionResponse -> {
                List<OperationResponse> operationResponses = fetchOperationsForTransaction(transactionResponse);
                return modelMapper.map(transactionResponse, operationResponses);
            })
            .map(mappedEntity -> apiClient.publish("/transactions", mappedEntity, Transaction.class))
            .subscribe(this::storeState, this::fatalAppState);
    }

    private List<OperationResponse> fetchOperationsForTransaction (TransactionResponse transactionResponse) {
        try {
            return horizonServer
                .operations()
                .forTransaction(transactionResponse.getHash())
                .execute()
                .getRecords();
        }
        catch (IOException ex) {
            return Collections.emptyList();
        }
    }

    private Mono<Long> retryBackOffPattern (Integer index) {
        return Mono.delay(Duration.ofMillis(index * 1000)); // todo configuration
    }

    private int retryCountPattern (Throwable error, Integer index) {
        if (index == 10) { // TODO is 10 tries fine? / configuration
            throw Exceptions.propagate(error);
        }
        return index;
    }

    private void storeState (BlockchainEntityWithState<Transaction> entityWithState) {
        LOG.info("Going to store state for entity {}", entityWithState);

        resourceStateRepository.saveAndFlush(
            ResourceState.from(
                entityWithState.getResourceState().getResourceType(),
                entityWithState.getResourceState().getPagingToken()
            )
        );
    }

    private void fatalAppState (Throwable throwable) {
        LOG.error("Fatal error when calling API", throwable);

        StellarIngestionModuleDemoApplication.shutdown();
    }

    private void subscribe (Consumer<TransactionResponse> responseConsumer) {
        String cursorPointer = resourceStateRepository
            .findById(Resource.LEDGER)
            .map(ResourceState::getPagingToken)
            .orElse("now");

        LOG.info("Transactions cursor is set to {}", cursorPointer);

        TransactionsRequestBuilder requestBuilder = horizonServer
            .transactions()
            .cursor(cursorPointer);

        testServerConnection();
        testCursorCorrectness(cursorPointer);

        requestBuilder.stream(responseConsumer::accept);
    }

    private void testServerConnection () {
        try {
            horizonServer.root().getProtocolVersion();
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot resolve connection to Horizon server", e);
        }
    }

    private void testCursorCorrectness (String cursorPointer) {
        try {
            horizonServer.transactions().cursor(cursorPointer).limit(1).execute();
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to test if cursor value is valid", e);
        }
    }
}
