package io.amberdata.ingestion.api.modules.stellar.configuration.subscribers;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class SubscriberErrorsHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SubscriberErrorsHandler.class);

    public static Flux<Long> onError (Flux<Throwable> companion) {
        return companion
            .doOnNext(throwable -> LOG.error("Error occurred: {}", throwable))
            .zipWith(Flux.range(1, Integer.MAX_VALUE), SubscriberErrorsHandler::retryCountPattern)
            .flatMap(SubscriberErrorsHandler::retryBackOffPattern);
    }

    private static Mono<Long> retryBackOffPattern (Integer index) {
        return Mono.delay(Duration.ofMillis(index * 1000)); // todo configuration
    }

    private static int retryCountPattern (Throwable error, Integer index) {
        if (index == 10) { // TODO is 10 tries fine? / configuration
            throw Exceptions.propagate(error);
        }
        return index;
    }
}
