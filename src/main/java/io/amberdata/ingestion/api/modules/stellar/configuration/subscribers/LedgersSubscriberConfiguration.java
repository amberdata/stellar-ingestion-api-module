package io.amberdata.ingestion.api.modules.stellar.configuration.subscribers;

import java.io.IOException;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.stellar.sdk.Server;
import org.stellar.sdk.requests.LedgersRequestBuilder;
import org.stellar.sdk.responses.LedgerResponse;

import io.amberdata.domain.Block;
import io.amberdata.ingestion.api.modules.stellar.StellarIngestionModuleDemoApplication;
import io.amberdata.ingestion.api.modules.stellar.client.IngestionApiClient;
import io.amberdata.ingestion.api.modules.stellar.mapper.ModelMapper;
import io.amberdata.ingestion.api.modules.stellar.state.ResourceStateStorage;
import io.amberdata.ingestion.api.modules.stellar.state.entities.Resource;

import javax.annotation.PostConstruct;
import reactor.core.publisher.Flux;

@Configuration
public class LedgersSubscriberConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(LedgersSubscriberConfiguration.class);

    private final ResourceStateStorage stateStorage;
    private final IngestionApiClient   apiClient;
    private final ModelMapper          modelMapper;
    private final Server               horizonServer;

    public LedgersSubscriberConfiguration (ResourceStateStorage stateStorage,
                                           IngestionApiClient apiClient,
                                           ModelMapper modelMapper,
                                           Server horizonServer) {

        this.stateStorage = stateStorage;
        this.apiClient = apiClient;
        this.modelMapper = modelMapper;
        this.horizonServer = horizonServer;
    }

    @PostConstruct
    public void createPipeline () {
        Flux.<LedgerResponse>push(sink -> subscribe(sink::next))
            .retryWhen(SubscriberErrorsHandler::onError)
            .map(modelMapper::map)
            .map(entity -> apiClient.publish("/blocks", entity, Block.class))
            .subscribe(stateStorage::storeState, this::fatalAppState);
    }

    private void fatalAppState (Throwable throwable) {
        LOG.error("Fatal error when calling API", throwable);

        StellarIngestionModuleDemoApplication.shutdown();
    }

    private void subscribe (Consumer<LedgerResponse> stellarSdkResponseConsumer) {
        String cursorPointer = stateStorage.getCursorPointer(Resource.LEDGER);

        LOG.info("Ledgers cursor is set to {}", cursorPointer);

        LedgersRequestBuilder ledgersRequest = horizonServer
            .ledgers()
            .cursor(cursorPointer);

        testServerConnection();
        testCursorCorrectness(cursorPointer);

        ledgersRequest.stream(stellarSdkResponseConsumer::accept);
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
            horizonServer.ledgers().cursor(cursorPointer).limit(1).execute();
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to test if cursor value is valid", e);
        }
    }
}
