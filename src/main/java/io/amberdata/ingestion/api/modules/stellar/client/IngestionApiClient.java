package io.amberdata.ingestion.api.modules.stellar.client;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import io.amberdata.domain.Block;
import io.amberdata.ingestion.api.modules.stellar.configuration.IngestionApiProperties;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class IngestionApiClient {
    private static final Logger LOG = LoggerFactory.getLogger(IngestionApiClient.class);

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

    public Block publish (Block block) {
        LOG.info("Going to publish block {}", block);

        return webClient
            .post()
            .uri("/blocks")
            .body(BodyInserters.fromObject(block))
            .retrieve()
            .bodyToMono(Block.class)
            .retryWhen(companion -> companion
                .doOnNext(throwable -> LOG.error("Error occurred: {}", throwable.getMessage()))
                .zipWith(Flux.range(1, 10), (error, index) -> index)
                .flatMap(index -> Mono.delay(Duration.ofMillis(index * 1000)))
            ).block();
    }

//    public Mono<Transaction> publish (Transaction transaction) {
//        return webClient
//            .post()
//            .uri("/transactions")
//            .retrieve()
//            .bodyToMono(Transaction.class);
//    }
}
