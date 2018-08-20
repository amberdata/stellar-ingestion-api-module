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

    private static int RETRIES_ON_ERROR;
    private static Duration BACKOFF_TIMEOUT_INITIAL_DURATION;
    private static Duration BACKOFF_TIMEOUT_MAX_DURATION;

    public SubscriberErrorsHandler (HorizonServerProperties serverProperties) {
        SubscriberErrorsHandler.RETRIES_ON_ERROR = serverProperties.getRetriesOnError() > 0 ?
            serverProperties.getRetriesOnError() : Integer.MAX_VALUE;
        SubscriberErrorsHandler.BACKOFF_TIMEOUT_INITIAL_DURATION = serverProperties.getBackOffTimeoutInitial();
        SubscriberErrorsHandler.BACKOFF_TIMEOUT_MAX_DURATION = serverProperties.getBackOffTimeoutMax();

        LOG.info(
            "Configuring Subscriber errors handler with re-tries: {}, " +
                "back-off-timeout-initial: {}ms, back-off-timeout-max {}ms",
            SubscriberErrorsHandler.RETRIES_ON_ERROR,
            SubscriberErrorsHandler.BACKOFF_TIMEOUT_INITIAL_DURATION.toMillis(),
            SubscriberErrorsHandler.BACKOFF_TIMEOUT_MAX_DURATION.toMillis());
    }

    public static Flux<Long> onError (Flux<Throwable> companion) {
        return companion
            .doOnNext(throwable -> LOG.error("Subscriber error occurred. Going to retry", throwable))
            .zipWith(Flux.range(1, Integer.MAX_VALUE), SubscriberErrorsHandler::retryCountPattern)
            .flatMap(SubscriberErrorsHandler::retryBackOffPattern);
    }

    private static Mono<Long> retryBackOffPattern (Integer index) {
        Duration delayDuration = BACKOFF_TIMEOUT_INITIAL_DURATION.multipliedBy(index);
        if (delayDuration.compareTo(BACKOFF_TIMEOUT_MAX_DURATION) > 0) {
            delayDuration = BACKOFF_TIMEOUT_MAX_DURATION;
        }
        LOG.info("Back-off delay {}ms", delayDuration.toMillis());

        return Mono.delay(delayDuration);
    }

    private static int retryCountPattern (Throwable error, Integer retryIndex) {
        ensureErrorIsNotFatal(error);

        LOG.info("Trying to recover after {}: {} times", error.getMessage(), retryIndex);
        if (retryIndex <= RETRIES_ON_ERROR) {
            return (int) Math.pow(2, retryIndex);
        }
        throw Exceptions.propagate(error);
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
