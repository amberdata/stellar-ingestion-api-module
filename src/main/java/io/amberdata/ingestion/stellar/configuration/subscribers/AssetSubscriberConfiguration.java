package io.amberdata.ingestion.stellar.configuration.subscribers;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.stellar.sdk.FormatException;
import org.stellar.sdk.responses.AssetResponse;
import org.stellar.sdk.responses.TransactionResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.ingestion.core.client.IngestionApiClient;
import io.amberdata.ingestion.core.state.ResourceStateStorage;
import io.amberdata.ingestion.domain.Asset;
import io.amberdata.ingestion.stellar.client.HorizonServer;
import io.amberdata.ingestion.stellar.mapper.ModelMapper;
import io.amberdata.ingestion.stellar.util.PreAuthTransactionProcessor;

import javax.annotation.PostConstruct;
import reactor.core.publisher.Flux;

@Configuration
@ConditionalOnProperty(prefix = "stellar", name = "subscribe-on-assets")
public class AssetSubscriberConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(AssetSubscriberConfiguration.class);

    private final ResourceStateStorage        stateStorage;
    private final IngestionApiClient          apiClient;
    private final ModelMapper                 modelMapper;
    private final HorizonServer               server;
    private final PreAuthTransactionProcessor preAuthTransactionProcessor;

    public AssetSubscriberConfiguration (ResourceStateStorage stateStorage,
                                         IngestionApiClient apiClient,
                                         ModelMapper modelMapper,
                                         HorizonServer server,
                                         PreAuthTransactionProcessor preAuthTransactionProcessor) {

        this.stateStorage = stateStorage;
        this.apiClient = apiClient;
        this.modelMapper = modelMapper;
        this.server = server;
        this.preAuthTransactionProcessor = preAuthTransactionProcessor;
    }

    @PostConstruct
    public void createPipeline () {
        LOG.info("Going to subscribe on Stellar Assets stream through Transactions stream");

        Flux.<TransactionResponse>create(sink -> subscribe(sink::next))
            .map(transactionResponse -> {
                List<OperationResponse> operationResponses = fetchOperationsForTransaction(transactionResponse);
                return processAssets(operationResponses, transactionResponse.getLedger()).stream()
                    .map(assetResponse -> modelMapper.map(assetResponse, transactionResponse.getPagingToken()))
                    .collect(Collectors.toList());
            })
            .map(entities -> apiClient.publish("/assets", entities, Asset.class))
            .subscribe(null, SubscriberErrorsHandler::handleFatalApplicationError);
    }

    private List<AssetResponse> processAssets (List<OperationResponse> operationResponses, Long ledger) {
        return modelMapper.mapAssets(operationResponses, ledger).stream()
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
        catch (FormatException ex) {
            return this.preAuthTransactionProcessor.fetchOperations(transactionResponse.getHash());
        }
        catch (IOException ex) {
            LOG.error("Unable to fetch information about operations for transaction {}", transactionResponse.getHash());
            return Collections.emptyList();
        }
    }

    private Optional<AssetResponse> fetchAsset (Asset asset) {
        try {
            List<AssetResponse> records = server
                .horizonServer()
                .assets()
                .assetCode(asset.getCode())
                .assetIssuer(asset.getIssuerAccount())
                .execute()
                .getRecords();

            if (records.size() > 0) {
                return Optional.of(records.get(0));
            }
        }
        catch (IOException ex) {
            LOG.error("Error during fetching an asset: " + asset.getCode());
        }

        return Optional.empty();
    }

    private void subscribe (Consumer<TransactionResponse> stellarSdkResponseConsumer) {
        String cursorPointer = stateStorage.getStateToken(Asset.class.getSimpleName(), () -> "now");

        LOG.info("Assets cursor is set to {} [using transactions cursor]", cursorPointer);

        server.testConnection();
        testCursorCorrectness(cursorPointer);

        server.horizonServer()
            .transactions()
            .cursor(cursorPointer)
            .stream(stellarSdkResponseConsumer::accept);
    }

    private void testCursorCorrectness (String cursorPointer) {
        try {
            server.horizonServer().transactions().cursor(cursorPointer).limit(1).execute();
        }
        catch (IOException e) {
            throw new HorizonServer.IncorrectRequestException("Failed to test if cursor value is valid", e);
        }
    }
}
