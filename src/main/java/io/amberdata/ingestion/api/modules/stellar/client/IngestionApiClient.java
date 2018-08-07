package io.amberdata.ingestion.api.modules.stellar.client;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

import reactor.core.Exceptions;
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
                                                     List<BlockchainEntityWithState<T>> entities,
                                                     Class<T> entityClass) {

        LOG.info("Going to publish {} to the ingestion API endpoint {}",
            String.join(",", entities.stream().map(Object::toString).collect(Collectors.toList())),
            endpointUri);

        List<T> domainEntities = entities.stream()
            .map(BlockchainEntityWithState::getEntity)
            .collect(Collectors.toList());

        webClient
            .post()
            .uri(endpointUri)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromObject(domainEntities))
            .retrieve()
            .bodyToMono(entityClass)
            .retryWhen(companion -> companion
                .doOnNext(throwable -> LOG.error("Error occurred: {}", throwable.getMessage()))
                .zipWith(Flux.range(1, Integer.MAX_VALUE), this::handleError)
                .flatMap(index -> Mono.delay(Duration.ofMillis(index * 100)))
            ).block();

        return entities.get(entities.size() - 1);
    }

    private int handleError (Throwable error, int retryIndex) {
        if (retryIndex < 100) { // todo make this configurable - number of retries before the app gives up
            return retryIndex + 1;
        }
        throw Exceptions.propagate(error);
    }

    public <T extends BlockchainEntity> BlockchainEntityWithState<T> publish (String endpointUri,
                                                                              BlockchainEntityWithState<T> entityWithState,
                                                                              Class<T> entityClass) {

        LOG.info("Going to publish {} to the ingestion API endpoint {}",  entityWithState.getEntity(), endpointUri);

        return publish(endpointUri, Collections.singletonList(entityWithState), entityClass);
    }
}
