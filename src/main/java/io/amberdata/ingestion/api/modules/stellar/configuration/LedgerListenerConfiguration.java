package io.amberdata.ingestion.api.modules.stellar.configuration;

import java.io.IOException;
import java.time.Duration;
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
import io.amberdata.ingestion.api.modules.stellar.state.Resource;
import io.amberdata.ingestion.api.modules.stellar.state.ResourceState;
import io.amberdata.ingestion.api.modules.stellar.state.ResourceStateRepository;

import javax.annotation.PostConstruct;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Configuration
public class LedgerListenerConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(LedgerListenerConfiguration.class);

    private final ResourceStateRepository resourceStateRepository;
    private final IngestionApiClient      apiClient;
    private final ModelMapper             modelMapper;

    public LedgerListenerConfiguration (ResourceStateRepository resourceStateRepository,
                                        IngestionApiClient apiClient,
                                        ModelMapper modelMapper) {
        this.resourceStateRepository = resourceStateRepository;
        this.apiClient = apiClient;
        this.modelMapper = modelMapper;
    }

    @PostConstruct
    public void createPipeline () {
        Flux.<LedgerResponse>push(sink -> subscribe(sink::next))
            .retryWhen(companion -> companion
                .doOnNext(throwable -> LOG.error("Error occurred: {}", throwable))
                .zipWith(Flux.range(1, Integer.MAX_VALUE), this::retryCountPattern)
                .flatMap(this::retryBackOffPattern)
            )
            .map(modelMapper::map)
            .map(apiClient::publish)
            .subscribe(this::storeState, this::fatalAppState);
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

    private void fatalAppState (Throwable throwable) {
        LOG.error("Fatal error when calling API", throwable);

        StellarIngestionModuleDemoApplication.shutdown();
    }

    private void storeState (Block block) {
        LOG.info("Going to store state for block {}", block);

        resourceStateRepository.saveAndFlush(
            ResourceState.from(
                Resource.LEDGER,
                block.getNumber().toString()
            )
        );
    }

    private void subscribe (Consumer<LedgerResponse> ledgerResponseConsumer) {
        String serverUrl = "https://horizon-testnet.stellar.org";
        Server server    = new Server(serverUrl);

        LOG.info("Connecting to server on {}", serverUrl);

        String cursorPointer = resourceStateRepository
            .findById(Resource.LEDGER)
            .map(ResourceState::getPagingToken)
            .orElse("now");

        LedgersRequestBuilder ledgersRequest = server
            .ledgers()
            .cursor(cursorPointer);

        testServerConnection(server);
        testRequestCorrectness(ledgersRequest);

        ledgersRequest.stream(ledgerResponseConsumer::accept);
    }

    private void testServerConnection (Server server) {
        try {
            server.root().getProtocolVersion();
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot resolve connection to Horizon server", e);
        }
    }

    private void testRequestCorrectness (LedgersRequestBuilder requestBuilder) {
        try {
            requestBuilder.limit(1).execute();
        }
        catch (IOException e) {
            throw new RuntimeException("Request seems to be incorrect", e);
        }
    }
}
