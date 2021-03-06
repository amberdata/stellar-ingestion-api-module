package io.amberdata.inbound.stellar.configuration.subscribers;

import io.amberdata.inbound.stellar.StellarInboundApplication;
import io.amberdata.inbound.stellar.configuration.properties.HorizonServerProperties;

import java.time.Duration;

import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import org.stellar.sdk.requests.TooManyRequestsException;

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

  /**
   * Default constructor.
   *
   * @param serverProperties the server properties
   */
  public SubscriberErrorsHandler(HorizonServerProperties serverProperties) {
    this.retriesOnError = serverProperties.getRetriesOnError() > 0
        ? serverProperties.getRetriesOnError()
        : Integer.MAX_VALUE;
    this.backoffTimeoutInitialDuration = serverProperties.getBackOffTimeoutInitial();
    this.backoffTimeoutMaxDuration     = serverProperties.getBackOffTimeoutMax();
    this.idleTimeoutMultiplier         = serverProperties.getIdleTimeoutMultiplier();

    LOG.info(
        "Configuring Subscriber errors handler with re-tries: {}, "
        + "back-off-timeout-initial: {}ms, back-off-timeout-max {}ms",
        this.retriesOnError,
        this.backoffTimeoutInitialDuration.toMillis(),
        this.backoffTimeoutMaxDuration.toMillis()
    );
  }

  /**
   * The error handler.
   *
   * @param companion the companion
   *
   * @return the next flux
   */
  public Flux<Long> onError(Flux<Throwable> companion) {
    return companion
      .doOnNext(throwable -> LOG.error("Subscriber error occurred. Going to retry", throwable))
      .zipWith(Flux.range(1, Integer.MAX_VALUE), this::duration)
      .flatMap(Mono::delay);
  }

  private Duration duration(Throwable error, Integer retryIndex) {
    this.ensureErrorIsNotFatal(error);

    if (error instanceof TooManyRequestsException) {
      int secondsToWait = ((TooManyRequestsException) error).getRetryAfter();
      LOG.info(
          "Horizon Rate Limit exceeded. As per the server's request waiting for {}sec",
          secondsToWait
      );
      return Duration.ofSeconds(secondsToWait);
    }

    if (error instanceof TimeoutException) {
      LOG.info(
          "{}. Going to wait {}ms before re-subscribe",
          error.getMessage(),
          this.backoffTimeoutInitialDuration.toMillis()
      );
      return this.backoffTimeoutInitialDuration;
    }

    LOG.info("Trying to recover after {}: {} times", error.getMessage(), retryIndex);

    if (retryIndex <= this.retriesOnError) {
      int multiplier = (int) Math.pow(2, retryIndex);

      Duration delay = this.backoffTimeoutInitialDuration.multipliedBy(multiplier);
      if (delay.compareTo(this.backoffTimeoutMaxDuration) > 0) {
        delay = this.backoffTimeoutMaxDuration;
      }
      LOG.info("Back-off delay {}ms", delay.toMillis());

      return delay;
    }
    throw Exceptions.propagate(error);
  }

  private void ensureErrorIsNotFatal(Throwable error) {
    if (error instanceof IllegalStateException) {
      LOG.error("Fatal error occurred. Check if there are any configuration issues", error);
      throw Exceptions.propagate(error);
    }
  }

  /**
   * Returns the timeout duration.
   *
   * @return the timeout duration.
   */
  public Duration timeoutDuration() {
    return this.backoffTimeoutMaxDuration.plus(
      Duration.ofMillis((long) (
        this.backoffTimeoutMaxDuration.toMillis() * this.idleTimeoutMultiplier
      ))
    );
  }

  /**
   * Handles the throwable.
   *
   * @param throwable the throwable to handle
   */
  public static void handleFatalApplicationError(Throwable throwable) {
    LOG.error("Fatal error when calling API", throwable);

    StellarInboundApplication.shutdown();
  }

}
