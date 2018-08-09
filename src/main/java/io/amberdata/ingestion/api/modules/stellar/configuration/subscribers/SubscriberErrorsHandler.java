package io.amberdata.ingestion.api.modules.stellar.configuration.subscribers;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.amberdata.ingestion.api.modules.stellar.StellarIngestionModuleDemoApplication;

import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class SubscriberErrorsHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SubscriberErrorsHandler.class);

    private static int retriesOnError;
    private static int backOffTimeout;

    public SubscriberErrorsHandler (@Value("${stellar.horizon.retries-on-error}") int retriesOnError,
                                    @Value("${stellar.horizon.back-off-timeout}") int backOffTimeout) {

        SubscriberErrorsHandler.retriesOnError = retriesOnError;
        SubscriberErrorsHandler.backOffTimeout = backOffTimeout;

        LOG.info(
            "Configuring Subscriber errors handler with re-tries: {}, back-off-timeout: {}ms",
            retriesOnError, backOffTimeout);
    }

    public static Flux<Long> onError (Flux<Throwable> companion) {
        return companion
            .doOnNext(throwable -> LOG.error("Subscriber error occurred. Going to retry", throwable))
            .zipWith(Flux.range(1, Integer.MAX_VALUE), SubscriberErrorsHandler::retryCountPattern)
            .flatMap(SubscriberErrorsHandler::retryBackOffPattern);
    }

    private static Mono<Long> retryBackOffPattern (Integer index) {
        return Mono.delay(Duration.ofMillis(index * backOffTimeout));
    }

    private static int retryCountPattern (Throwable error, Integer index) {
        LOG.info("Retrying to recover after {}: {} times", error.getMessage(), index);
        if (index == retriesOnError) {
            throw Exceptions.propagate(error);
        }
        return index;
    }

    public static void handleFatalApplicationError (Throwable throwable) {
        LOG.error("Fatal error when calling API", throwable);

        StellarIngestionModuleDemoApplication.shutdown();
    }
}
