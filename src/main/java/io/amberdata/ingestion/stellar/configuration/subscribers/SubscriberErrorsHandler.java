package io.amberdata.ingestion.stellar.configuration.subscribers;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.amberdata.ingestion.stellar.StellarIngestionModuleDemoApplication;
import io.amberdata.ingestion.stellar.configuration.properties.HorizonServerProperties;

import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class SubscriberErrorsHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SubscriberErrorsHandler.class);

    private static int retriesOnError;
    private static Duration backOffTimeoutInitialDuration;
    private static Duration backOffTimeoutMaxDuration;

    public SubscriberErrorsHandler (HorizonServerProperties serverProperties) {
        SubscriberErrorsHandler.retriesOnError = serverProperties.getRetriesOnError() > 0 ?
            serverProperties.getRetriesOnError() : Integer.MAX_VALUE;
        SubscriberErrorsHandler.backOffTimeoutInitialDuration = serverProperties.getBackOffTimeoutInitial();
        SubscriberErrorsHandler.backOffTimeoutMaxDuration = serverProperties.getBackOffTimeoutMax();

        LOG.info(
            "Configuring Subscriber errors handler with re-tries: {}, " +
                "back-off-timeout-initial: {}ms, back-off-timeout-max {}ms",
            SubscriberErrorsHandler.retriesOnError,
            SubscriberErrorsHandler.backOffTimeoutInitialDuration.toMillis(),
            SubscriberErrorsHandler.backOffTimeoutMaxDuration.toMillis());
    }

    public static Flux<Long> onError (Flux<Throwable> companion) {
        return companion
            .doOnNext(throwable -> LOG.error("Subscriber error occurred. Going to retry", throwable))
            .zipWith(Flux.range(1, Integer.MAX_VALUE), SubscriberErrorsHandler::retryCountPattern)
            .flatMap(SubscriberErrorsHandler::retryBackOffPattern);
    }

    private static Mono<Long> retryBackOffPattern (Integer index) {
        Duration delayDuration = backOffTimeoutInitialDuration.multipliedBy(index);
        if (delayDuration.compareTo(backOffTimeoutMaxDuration) > 0) {
            delayDuration = backOffTimeoutMaxDuration;
        }
        LOG.info("Back-off delay {}ms", delayDuration.toMillis());

        return Mono.delay(delayDuration);
    }

    private static int retryCountPattern (Throwable error, Integer retryIndex) {
        ensureErrorIsNotFatal(error);

        LOG.info("Trying to recover after {}: {} times", error.getMessage(), retryIndex);
        if (retryIndex == retriesOnError) {
            throw Exceptions.propagate(error);
        }
        return retryIndex;
    }

    private static void ensureErrorIsNotFatal (Throwable error) {
        if (error instanceof IllegalStateException) {
            LOG.error("Fatal error occurred. Check if there are any configuration issues", error);
            throw Exceptions.propagate(error);
        }
    }

    public static void handleFatalApplicationError (Throwable throwable) {
        LOG.error("Fatal error when calling API", throwable);

        StellarIngestionModuleDemoApplication.shutdown();
    }
}
