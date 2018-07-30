package io.amberdata.ingestion.api.modules.stellar.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import io.amberdata.domain.Block;
import io.amberdata.ingestion.api.modules.stellar.configuration.IngestionApiProperties;

import reactor.core.publisher.Mono;

@Component
public class IngestionApiClient {

    private final WebClient              webClient;
    private final IngestionApiProperties apiProperties;

    public IngestionApiClient (IngestionApiProperties apiProperties) {
        this.apiProperties = apiProperties;
        this.webClient = WebClient.builder()
            .baseUrl(apiProperties.getUrl())
            .defaultHeaders(this::defaultHttpHeaders)
            .build();
    }

    private void defaultHttpHeaders (HttpHeaders httpHeaders) {
        httpHeaders.add("x-amberdata-blockchain-id", apiProperties.getBlockchainId());
        httpHeaders.add("x-amberdata-api-key", apiProperties.getApiKey());
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
    }

    public Mono<Block> publish (Mono<Block> blockMono) {
        return webClient
            .post()
            .uri("/blocks")
            .body(blockMono, Block.class)
            .retrieve()
            .bodyToMono(Block.class);
    }

//    public Mono<Transaction> publish (Transaction transaction) {
//        return webClient
//            .post()
//            .uri("/transactions")
//            .retrieve()
//            .bodyToMono(Transaction.class);
//    }
}
