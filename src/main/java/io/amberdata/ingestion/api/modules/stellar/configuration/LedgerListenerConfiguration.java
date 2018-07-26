package io.amberdata.ingestion.api.modules.stellar.configuration;

import java.util.function.Consumer;

import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.stellar.sdk.Server;
import org.stellar.sdk.responses.LedgerResponse;

import javax.annotation.PostConstruct;
import reactor.core.publisher.Flux;

@Configuration
public class LedgerListenerConfiguration {
    private Consumer<LedgerResponse> ledgerResponseConsumer;

    @PostConstruct
    public void createPipeline () {
        Flux.<LedgerResponse>create(sink -> registerListener(sink::next))
            .buffer(5)
            .subscribe(
                System.out::println, // here will be service call to push data to the Ingestion API
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
