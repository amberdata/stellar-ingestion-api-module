package io.amberdata.ingestion.api.modules.stellar.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import io.amberdata.domain.Block;
import io.amberdata.domain.Transaction;

@Component
public class IngestionApiClient {
    private final RestTemplate restTemplate;
    private final String ingestionApiUrl;

    public IngestionApiClient (
            @Value("${ingestion.api.url}") String ingestionApiUrl, 
            RestTemplateBuilder restTemplateBuilder) {

        this.ingestionApiUrl = ingestionApiUrl;
        this.restTemplate    = restTemplateBuilder.build();
    }

    public Block publish (Block block) {
        return restTemplate.postForObject(ingestionApiUrl, null, Block.class);
    }

    public Transaction publish (Transaction transaction) {
        return restTemplate.postForObject(ingestionApiUrl, null, Transaction.class);
    }
}
