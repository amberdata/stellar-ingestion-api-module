package io.amberdata.ingestion.stellar.configuration.subscribers;

import io.amberdata.ingestion.core.client.IngestionApiClient;
import io.amberdata.ingestion.core.state.ResourceStateStorage;
import java.io.IOException;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.stellar.sdk.responses.LedgerResponse;

import io.amberdata.ingestion.domain.Block;
import io.amberdata.ingestion.stellar.client.HorizonServer;
import io.amberdata.ingestion.stellar.configuration.history.HistoricalManager;
import io.amberdata.ingestion.stellar.configuration.properties.BatchSettings;
import io.amberdata.ingestion.stellar.mapper.ModelMapper;

import javax.annotation.PostConstruct;
import reactor.core.publisher.Flux;

@Configuration
@ConditionalOnProperty(prefix = "stellar", name="subscribe-on-ledgers")
public class LedgersSubscriberConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(LedgersSubscriberConfiguration.class);

    private final ResourceStateStorage stateStorage;
    private final IngestionApiClient   apiClient;
    private final ModelMapper          modelMapper;
    private final HistoricalManager    historicalManager;
    private final HorizonServer        server;
    private final BatchSettings        batchSettings;

    public LedgersSubscriberConfiguration (ResourceStateStorage stateStorage,
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
        LOG.info("Going to subscribe on Stellar Ledgers stream");

        Flux.<LedgerResponse>push(sink -> subscribe(sink::next))
            .retryWhen(SubscriberErrorsHandler::onError)
            .doOnNext(l -> LOG.info("Received ledger with sequence {}", l.getSequence()))
            .map(modelMapper::map)
            .buffer(Integer.parseInt(batchSettings.getBlocksInChunk()))
            .map(entities -> apiClient.publish("/blocks", entities))
            .subscribe(null, SubscriberErrorsHandler::handleFatalApplicationError);
    }

    private void subscribe (Consumer<LedgerResponse> stellarSdkResponseConsumer) {
        String cursorPointer = getCursorPointer();

        LOG.info("Ledgers cursor is set to {}", cursorPointer);

        server.testConnection();
        testCursorCorrectness(cursorPointer);

        server.horizonServer()
            .ledgers()
            .cursor(cursorPointer)
            .stream(stellarSdkResponseConsumer::accept);
    }

    private String getCursorPointer () {
        if (historicalManager.disabled()) {
            return stateStorage.getStateToken(Block.class.getSimpleName(), () -> "now");
        } else {
            return historicalManager.ledgerPagingToken();
        }
    }

    private void testCursorCorrectness (String cursorPointer) {
        try {
            server.horizonServer().ledgers().cursor(cursorPointer).limit(1).execute();
        }
        catch (IOException e) {
            throw new HorizonServer.IncorrectRequestException("Failed to test if cursor value is valid", e);
        }
    }
}
