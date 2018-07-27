package io.amberdata.ingestion.api.modules.stellar.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("x-amberdata-blockchain-id", "f6d90419722d7691");
        httpHeaders.add("x-amberdata-api-key", "0c866e124988b1bc994bbfb4e50a5289");

        return restTemplate.exchange(ingestionApiUrl + "/blocks", HttpMethod.POST,
            new HttpEntity<Block>(httpHeaders), Block.class).getBody();
    }

    public Transaction publish (Transaction transaction) {
        return restTemplate.postForObject(ingestionApiUrl+ "/transactions", null, Transaction.class);
    }
}
