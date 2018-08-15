package io.amberdata.ingestion.stellar.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("ingestion.api.batch")
public class BatchSettings {
    private String blocksInChunk;
    private String transactionsInChunk;

    public String getBlocksInChunk () {
        return blocksInChunk;
    }

    public void setBlocksInChunk (String blocksInChunk) {
        this.blocksInChunk = blocksInChunk;
    }

    public String getTransactionsInChunk () {
        return transactionsInChunk;
    }

    public void setTransactionsInChunk (String transactionsInChunk) {
        this.transactionsInChunk = transactionsInChunk;
    }

    @Override
    public String toString () {
        return "BatchSettings{" +
            "blocksInChunk='" + blocksInChunk + '\'' +
            ", transactionsInChunk='" + transactionsInChunk + '\'' +
            '}';
    }
}
