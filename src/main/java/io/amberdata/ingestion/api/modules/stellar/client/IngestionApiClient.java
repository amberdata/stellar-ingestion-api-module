package io.amberdata.ingestion.api.modules.stellar.client;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import io.amberdata.domain.BlockchainEntity;
import io.amberdata.ingestion.api.modules.stellar.configuration.properties.IngestionApiProperties;
import io.amberdata.ingestion.api.modules.stellar.state.entities.BlockchainEntityWithState;

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

    public <T extends BlockchainEntity> BlockchainEntityWithState<T> publish (String endpointUri,
                                                     BlockchainEntityWithState<T> entityWithState,
                                                     Class<T> entityClass) {
        LOG.info("Going to publish {} to the ingestion API endpoint {}",  entityWithState.getEntity(), endpointUri);

        webClient
            .post()
            .uri(endpointUri)
            .body(BodyInserters.fromObject(entityWithState.getEntity()))
            .retrieve()
            .bodyToMono(entityClass)
            .retryWhen(companion -> companion
                .doOnNext(throwable -> LOG.error("Error occurred: {}", throwable.getMessage()))
                .zipWith(Flux.range(1, 10), (error, index) -> index)
                .flatMap(index -> Mono.delay(Duration.ofMillis(index * 1000)))
            ).block();

        return entityWithState;
    }
}
