package io.amberdata.ingestion.api.modules.stellar.configuration.properties;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@ConfigurationProperties("ingestion.api")
public class IngestionApiProperties {
    private String url;
    private String blockchainId;
    private String apiKey;

    private Integer  retriesOnError;

    @DurationUnit(ChronoUnit.MILLIS)
    private Duration backOffTimeoutInitial;
    @DurationUnit(ChronoUnit.MILLIS)
    private Duration backOffTimeoutMax;

    private Batch batch;

    public String getUrl () {
        return url;
    }

    public void setUrl (String url) {
        this.url = url;
    }

    public String getBlockchainId () {
        return blockchainId;
    }

    public void setBlockchainId (String blockchainId) {
        this.blockchainId = blockchainId;
    }

    public String getApiKey () {
        return apiKey;
    }

    public void setApiKey (String apiKey) {
        this.apiKey = apiKey;
    }

    public Integer getRetriesOnError () {
        return retriesOnError > 0 ? retriesOnError : Integer.MAX_VALUE;
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

    public Batch getBatch () {
        return batch;
    }

    public void setBatch (Batch batch) {
        this.batch = batch;
    }

    public static class Batch {
        private Integer blocksInChunk;
        private Integer transactionsInChunk;

        public Integer getBlocksInChunk () {
            return blocksInChunk;
        }

        public void setBlocksInChunk (Integer blocksInChunk) {
            this.blocksInChunk = blocksInChunk;
        }

        public Integer getTransactionsInChunk () {
            return transactionsInChunk;
        }

        public void setTransactionsInChunk (Integer transactionsInChunk) {
            this.transactionsInChunk = transactionsInChunk;
        }

        @Override
        public String toString () {
            return "Batch{" +
                "blocksInChunk=" + blocksInChunk +
                ", transactionsInChunk=" + transactionsInChunk +
                '}';
        }
    }

    @Override
    public String toString () {
        return "IngestionApiProperties{" +
            "url='" + url + '\'' +
            ", blockchainId='" + blockchainId + '\'' +
            ", apiKey='" + apiKey + '\'' +
            ", retriesOnError=" + retriesOnError +
            ", batch=" + batch +
            '}';
    }
}
