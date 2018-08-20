package io.amberdata.ingestion.stellar.configuration.subscribers;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.stellar.sdk.FormatException;
import org.stellar.sdk.responses.AssetResponse;
import org.stellar.sdk.responses.TransactionResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.ingestion.core.client.BlockchainEntityWithState;
import io.amberdata.ingestion.core.client.IngestionApiClient;
import io.amberdata.ingestion.core.state.ResourceStateStorage;
import io.amberdata.ingestion.core.state.entities.ResourceState;
import io.amberdata.ingestion.domain.Asset;
import io.amberdata.ingestion.domain.Transaction;
import io.amberdata.ingestion.stellar.client.HorizonServer;
import io.amberdata.ingestion.stellar.configuration.history.HistoricalManager;
import io.amberdata.ingestion.stellar.configuration.properties.BatchSettings;
import io.amberdata.ingestion.stellar.mapper.ModelMapper;

import javax.annotation.PostConstruct;
import reactor.core.publisher.Flux;

@Configuration
@ConditionalOnProperty(prefix = "stellar", name = "subscribe-on-assets")
public class AssetSubscriberConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(AssetSubscriberConfiguration.class);

    private final ResourceStateStorage stateStorage;
    private final IngestionApiClient   apiClient;
    private final ModelMapper          modelMapper;
    private final HistoricalManager    historicalManager;
    private final HorizonServer        server;
    private final BatchSettings        batchSettings;

    public AssetSubscriberConfiguration (ResourceStateStorage stateStorage,
                                         IngestionApiClient apiClient,
                                         ModelMapper modelMapper,
                                         HistoricalManager historicalManager,
                                         HorizonServer server,
                                         BatchSettings batchSettings) {
        this.stateStorage = stateStorage;
        this.apiClient = apiClient;
        this.modelMapper = modelMapper;
        this.historicalManager = historicalManager;
        this.server = server;
        this.batchSettings = batchSettings;
    }

    @PostConstruct
    public void createPipeline () {
        LOG.info("Going to subscribe on Stellar Assets stream through Transactions stream");

        Flux.<TransactionResponse>create(sink -> subscribe(sink::next))
            .map(transactionResponse -> {
                List<OperationResponse> operationResponses = fetchOperationsForTransaction(transactionResponse);
                return processAssets(operationResponses, transactionResponse.getLedger()).stream()
                    .map(asset -> BlockchainEntityWithState.from(
                        asset,
                        ResourceState.from(Asset.class.getSimpleName(), transactionResponse.getPagingToken())
                    ));
            })
            .flatMap(Flux::fromStream)
            .buffer(this.batchSettings.assetsInChunk())
            .retryWhen(SubscriberErrorsHandler::onError)
            .subscribe(
                entities -> this.apiClient.publish("/assets", entities),
                SubscriberErrorsHandler::handleFatalApplicationError
            );
    }

    private List<Asset> processAssets (List<OperationResponse> operationResponses, Long ledger) {
        List<Asset> assets = modelMapper.mapAssets(operationResponses, ledger).stream()
            .distinct()
            .collect(Collectors.toList());
        assets.forEach(this::enrichAsset);
        return assets;
    }

    private List<OperationResponse> fetchOperationsForTransaction (TransactionResponse transactionResponse) {
        try {
            return this.server.horizonServer()
                .operations()
                .forTransaction(transactionResponse.getHash())
                .execute()
                .getRecords();
        }
        catch (IOException | FormatException ex) {
            LOG.error("Unable to fetch information about operations for transaction {}", transactionResponse.getHash());
            return Collections.emptyList();
        }
    }

    private void enrichAsset (Asset asset) {
        try {
            List<AssetResponse> records = this.server
                .horizonServer()
                .assets()
                .assetCode(asset.getCode())
                .assetIssuer(asset.getIssuerAccount())
                .execute()
                .getRecords();

            if (records.size() > 0) {
                AssetResponse assetResponse = records.get(0);
                asset.setType(Asset.AssetType.fromName(assetResponse.getAssetType()));
                asset.setCode(assetResponse.getAssetCode());
                asset.setIssuerAccount(assetResponse.getAssetIssuer());
                asset.setAmount(assetResponse.getAmount() != null ? assetResponse.getAmount() : "0");
                asset.setMeta(assetOptionalProperties(assetResponse));
            } else {
                asset.setAmount("0");
            }
        }
        catch (Exception ex) {
            asset.setAmount("0");
            LOG.error("Error during fetching an asset: " + asset.getCode());
        }
    }

    private Map<String, Object> assetOptionalProperties (AssetResponse assetResponse) {
        Map<String, Object> optionalProperties = new HashMap<>();

        optionalProperties.put("num_accounts", assetResponse.getNumAccounts());
        optionalProperties.put("flag_auth_required", assetResponse.getFlags().isAuthRequired());
        optionalProperties.put("flag_auth_revocable", assetResponse.getFlags().isAuthRevocable());

        return optionalProperties;
    }

    private void subscribe (Consumer<TransactionResponse> stellarSdkResponseConsumer) {
        String cursorPointer = getCursorPointer();

        LOG.info("Assets cursor is set to {} [using transactions cursor]", cursorPointer);

        this.server.testConnection();
        testCursorCorrectness(cursorPointer);

        this.server.horizonServer()
            .transactions()
            .cursor(cursorPointer)
            .stream(stellarSdkResponseConsumer::accept);
    }

    private String getCursorPointer () {
        if (this.historicalManager.disabled()) {
            return this.stateStorage.getStateToken(Transaction.class.getSimpleName(), () -> "now");
        } else {
            return this.historicalManager.transactionPagingToken();
        }
    }

    private void testCursorCorrectness (String cursorPointer) {
        try {
            this.server.horizonServer().transactions().cursor(cursorPointer).limit(1).execute();
        }
        catch (IOException e) {
            throw new HorizonServer.IncorrectRequestException("Failed to test if cursor value is valid", e);
        }
    }
}
