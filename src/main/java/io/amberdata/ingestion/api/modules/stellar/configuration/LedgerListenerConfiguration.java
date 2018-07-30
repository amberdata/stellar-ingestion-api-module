package io.amberdata.ingestion.api.modules.stellar.configuration;

import java.time.Duration;
import java.time.LocalTime;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.stellar.sdk.Server;
import org.stellar.sdk.responses.LedgerResponse;

import io.amberdata.ingestion.api.modules.stellar.client.IngestionApiClient;
import io.amberdata.ingestion.api.modules.stellar.mapper.ModelMapper;

import javax.annotation.PostConstruct;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Configuration
public class LedgerListenerConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(LedgerListenerConfiguration.class);

    private final IngestionApiClient apiClient;
    private final ModelMapper        modelMapper;

    private Consumer<LedgerResponse> ledgerResponseConsumer;

    public LedgerListenerConfiguration (IngestionApiClient apiClient, ModelMapper modelMapper) {
        this.apiClient = apiClient;
        this.modelMapper = modelMapper;
    }

    @PostConstruct
    public void createPipeline () {
        Flux.<LedgerResponse>create(sink -> registerListener(sink::next))
            .map(modelMapper::map)
            .map(Mono::just)
            .retryWhen(companion -> companion
                .doOnNext(s -> System.out.println(s + " at " + LocalTime.now()))
                .zipWith(Flux.range(1, 100), (error, index) -> {
                    if (index < 100) {
                        return index;
                    }
                    else {
                        throw Exceptions.propagate(error);
                    }
                })
                .flatMap(index -> Mono.delay(Duration.ofMillis(index * 100)))
                .doOnNext(s -> System.out.println("retried at " + LocalTime.now()))
            )
            .map(apiClient::publish)
            .subscribe(
                blockMono -> LOG.info("API responded with object {}", blockMono.block()),
                System.err::println
            );
    }

    @PostConstruct
    public void subscribeOnLedgers () {
        Server server = new Server("https://horizon-testnet.stellar.org");
//        Server server = new Server("http://localhost:8000");

        server
            .ledgers()
            .cursor("now")
            .stream(ledgerResponse -> ledgerResponseConsumer.accept(ledgerResponse));
    }

    private void registerListener (Consumer<LedgerResponse> ledgerResponseConsumer) {
        this.ledgerResponseConsumer = ledgerResponseConsumer;
    }
}
