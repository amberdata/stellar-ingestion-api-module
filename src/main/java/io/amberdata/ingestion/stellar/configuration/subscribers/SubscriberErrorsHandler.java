package io.amberdata.ingestion.stellar.configuration.subscribers;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeoutException;

import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.stellar.sdk.requests.TooManyRequestsException;

import io.amberdata.ingestion.stellar.StellarIngestionModuleDemoApplication;
import io.amberdata.ingestion.stellar.configuration.properties.HorizonServerProperties;

import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class SubscriberErrorsHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SubscriberErrorsHandler.class);

    private final int      retriesOnError;
    private final double   idleTimeoutMultiplier;
    private final Duration backoffTimeoutInitialDuration;
    private final Duration backoffTimeoutMaxDuration;

    public SubscriberErrorsHandler (HorizonServerProperties serverProperties) {
        this.retriesOnError = serverProperties.getRetriesOnError() > 0 ?
            serverProperties.getRetriesOnError() : Integer.MAX_VALUE;
        this.backoffTimeoutInitialDuration = serverProperties.getBackOffTimeoutInitial();
        this.backoffTimeoutMaxDuration = serverProperties.getBackOffTimeoutMax();
        this.idleTimeoutMultiplier = serverProperties.getIdleTimeoutMultiplier();

        LOG.info(
            "Configuring Subscriber errors handler with re-tries: {}, " +
                "back-off-timeout-initial: {}ms, back-off-timeout-max {}ms",
            this.retriesOnError,
            this.backoffTimeoutInitialDuration.toMillis(),
            this.backoffTimeoutMaxDuration.toMillis());
    }

    public Flux<Long> onError (Flux<Throwable> companion) {
        return companion
            .doOnNext(throwable -> LOG.error("Subscriber error occurred. Going to retry", throwable))
            .zipWith(Flux.range(1, Integer.MAX_VALUE), this::duration)
            .flatMap(Mono::delay);
    }

    private Duration duration (Throwable error, Integer retryIndex) {
        ensureErrorIsNotFatal(error);

        if (error instanceof TooManyRequestsException) {
            int secondsToWait = ((TooManyRequestsException) error).getRetryAfter();
            LOG.info("Horizon Rate Limit exceeded. As per the server's request waiting {}sec", secondsToWait);
            return Duration.of(secondsToWait, ChronoUnit.SECONDS);
        }

        if (error instanceof TimeoutException) {
            LOG.info("{}. Going to wait {}ms before re-subscribe", error.getMessage(), backoffTimeoutInitialDuration.toMillis());
            return backoffTimeoutInitialDuration;
        }

        LOG.info("Trying to recover after {}: {} times", error.getMessage(), retryIndex);

        if (retryIndex <= retriesOnError) {
            int multiplier = (int) Math.pow(2, retryIndex);

            Duration delay = backoffTimeoutInitialDuration.multipliedBy(multiplier);
            if (delay.compareTo(backoffTimeoutMaxDuration) > 0) {
                delay = backoffTimeoutMaxDuration;
            }
            LOG.info("Back-off delay {}ms", delay.toMillis());

            return delay;
        }
        throw Exceptions.propagate(error);
    }

    private void ensureErrorIsNotFatal (Throwable error) {
        if (error instanceof IllegalStateException) {
            LOG.error("Fatal error occurred. Check if there are any configuration issues", error);
            throw Exceptions.propagate(error);
        }
    }

    public Duration timeoutDuration () {
        return backoffTimeoutMaxDuration.plus(
            Duration.ofMillis((long) (backoffTimeoutMaxDuration.toMillis() * idleTimeoutMultiplier))
        );
    }

    public static void handleFatalApplicationError (Throwable throwable) {
        LOG.error("Fatal error when calling API", throwable);

        StellarIngestionModuleDemoApplication.shutdown();
    }
}
