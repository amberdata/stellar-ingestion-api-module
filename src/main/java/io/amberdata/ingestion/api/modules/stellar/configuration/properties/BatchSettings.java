package io.amberdata.ingestion.api.modules.stellar.configuration.properties;

import org.springframework.stereotype.Component;

@Component
public class BatchSettings {

    private final IngestionApiProperties apiProperties;

    public BatchSettings (IngestionApiProperties apiProperties) {
        this.apiProperties = apiProperties;
    }

    public int blocksInChunk () {
        return apiProperties.getBatch().getBlocksInChunk();
    }

    public int transactionsInChunk () {
        return apiProperties.getBatch().getTransactionsInChunk();
    }
}
