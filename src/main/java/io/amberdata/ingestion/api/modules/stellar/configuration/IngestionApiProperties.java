package io.amberdata.ingestion.api.modules.stellar.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("ingestion.api")
public class IngestionApiProperties {
    private String url;
    private String blockchainId;
    private String apiKey;

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
}
