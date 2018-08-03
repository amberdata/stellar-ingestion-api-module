package io.amberdata.ingestion.api.modules.stellar.configuration;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.stellar.sdk.responses.AssetResponse;
import org.stellar.sdk.responses.TransactionResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.domain.Asset;
import io.amberdata.ingestion.api.modules.stellar.client.HorizonServer;
import io.amberdata.ingestion.api.modules.stellar.client.IngestionApiClient;
import io.amberdata.ingestion.api.modules.stellar.mapper.ModelMapper;

import javax.annotation.PostConstruct;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Configuration
public class AssetListenerConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(AssetListenerConfiguration.class);

    private final IngestionApiClient apiClient;
    private final ModelMapper        modelMapper;
    private final HorizonServer      server;

    private Consumer<TransactionResponse> transactionResponseConsumer;

    public AssetListenerConfiguration (IngestionApiClient apiClient,
                                       ModelMapper modelMapper,
                                       HorizonServer server) {
        this.apiClient = apiClient;
        this.modelMapper = modelMapper;
        this.server = server;
    }

    @PostConstruct
    public void createPipeline () {
        Flux.<TransactionResponse>create(sink -> registerListener(sink::next))
            .map(transactionResponse -> {
                List<OperationResponse> operationResponses = fetchOperationsForTransaction(transactionResponse);
                return processAssets(operationResponses).stream()
                    .map(modelMapper::map);
            })
            .map(Mono::just)
            .subscribe(
                assetMono -> LOG.info("API responded with object {}", assetMono.block()),
                Throwable::printStackTrace
            );
    }

    private List<AssetResponse> processAssets (List<OperationResponse> operationResponses) {
        return modelMapper.mapAssets(operationResponses).stream()
            .distinct()
            .map(this::fetchAsset)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    private List<OperationResponse> fetchOperationsForTransaction (TransactionResponse transactionResponse) {
        try {
            return server.horizonServer()
                .operations()
                .forTransaction(transactionResponse.getHash())
                .execute()
                .getRecords();
        }
        catch (IOException ex) {
            return Collections.emptyList();
        }
    }

    @PostConstruct
    public void subscribeOnTransactions () {
        server.horizonServer()
            .transactions()
            .cursor("now")
            .stream(transactionResponse -> transactionResponseConsumer.accept(transactionResponse));
    }

    private Optional<AssetResponse> fetchAsset (Asset asset) {
        try {
            return Optional.of(
                server.horizonServer()
                    .assets()
                    .assetCode(asset.getCode())
                    .assetIssuer(asset.getIssuerAccount())
                    .execute()
                    .getRecords()
                    .get(0)
            );
        }
        catch (IOException ex) {
            return Optional.empty();
        }
    }

    private void registerListener (Consumer<TransactionResponse> transactionResponseConsumer) {
        this.transactionResponseConsumer = transactionResponseConsumer;
    }
}
