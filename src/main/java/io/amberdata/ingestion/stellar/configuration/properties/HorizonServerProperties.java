package io.amberdata.ingestion.stellar.configuration.properties;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("stellar.horizon")
public class HorizonServerProperties {
    private String server;
    private Integer retriesOnError;

    @DurationUnit(ChronoUnit.MILLIS)
    private Duration backOffTimeoutInitial;

    @DurationUnit(ChronoUnit.MILLIS)
    private Duration backOffTimeoutMax;

    public String getServer () {
        return server;
    }

    public void setServer (String server) {
        this.server = server;
    }

    public Integer getRetriesOnError () {
        return retriesOnError;
    }

    public void setRetriesOnError (Integer retriesOnError) {
        this.retriesOnError = retriesOnError;
    }

    public Duration getBackOffTimeoutInitial () {
        return backOffTimeoutInitial;
    }

    public void setBackOffTimeoutInitial (Duration backOffTimeoutInitial) {
        this.backOffTimeoutInitial = backOffTimeoutInitial;
    }

    public Duration getBackOffTimeoutMax () {
        return backOffTimeoutMax;
    }

    public void setBackOffTimeoutMax (Duration backOffTimeoutMax) {
        this.backOffTimeoutMax = backOffTimeoutMax;
    }
}

