package io.amberdata.ingestion.api.modules.stellar.client;

import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import io.amberdata.domain.Block;
import io.amberdata.domain.Transaction;

import reactor.core.publisher.Mono;

@Component
public class IngestionApiClient {

    private final WebClient webClient;

    public IngestionApiClient (@Value("${ingestion.api.url}") String ingestionApiUrl) {
        webClient = WebClient.builder()
            .baseUrl(ingestionApiUrl)
            .defaultHeaders(this::configureApiHeaders)
            .build();
    }

    private void configureApiHeaders(HttpHeaders httpHeaders) {
        httpHeaders.add("x-amberdata-blockchain-id", "f6d90419722d7691");
        httpHeaders.add("x-amberdata-api-key", "0c866e124988b1bc994bbfb4e50a5289");
    }

    public Mono<Block> publish (Block block) {
        return webClient
            .post()
            .uri("/blocks")
            .retrieve()
            .bodyToMono(Block.class);
    }

    public Mono<Transaction> publish (Transaction transaction) {
        return webClient
            .post()
            .uri("/transactions")
            .retrieve()
            .bodyToMono(Transaction.class);
    }
}
