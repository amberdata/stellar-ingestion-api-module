package io.amberdata.ingestion.api.modules.stellar.configuration;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.stellar.sdk.Server;
import org.stellar.sdk.responses.LedgerResponse;

import io.amberdata.domain.Block;
import io.amberdata.ingestion.api.modules.stellar.client.IngestionApiClient;
import io.amberdata.ingestion.api.modules.stellar.mapper.ModelMapper;

import javax.annotation.PostConstruct;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Configuration
public class LedgerListenerConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(LedgerListenerConfiguration.class);

    private final IngestionApiClient apiClient;
    private final ModelMapper        modelMapper;

    public LedgerListenerConfiguration (IngestionApiClient apiClient, ModelMapper modelMapper) {
        this.apiClient = apiClient;
        this.modelMapper = modelMapper;
    }

    @PostConstruct
    public void createPipeline () {
        Flux.<LedgerResponse>create(sink -> subscribe(sink::next))
            .map(modelMapper::map)
            .map(apiClient::publish)
            .subscribe(this::storeState, this::handleError);
    }

    private void handleError (Throwable throwable) {
        LOG.error("Fatal error when calling API", throwable);
    }

    private void storeState (Mono<Block> blockMono) {
        Block block = blockMono.block();
        LOG.info("Going to store state for block {}", block);
        // TODO
    }

    private void subscribe (Consumer<LedgerResponse> ledgerResponseConsumer) {
        Server server = new Server("https://horizon-testnet.stellar.org");
        server
            .ledgers()
            .cursor("now")
            .stream(ledgerResponseConsumer::accept);
    }
}
