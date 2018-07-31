package io.amberdata.ingestion.api.modules.stellar.configuration;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.stellar.sdk.Server;
import org.stellar.sdk.responses.TransactionResponse;
import org.stellar.sdk.responses.operations.CreateAccountOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.ingestion.api.modules.stellar.client.IngestionApiClient;
import io.amberdata.ingestion.api.modules.stellar.mapper.ModelMapper;

@Configuration
public class TransactionListenerConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionListenerConfiguration.class);

    private final IngestionApiClient apiClient;
    private final ModelMapper        modelMapper;
    private final Server horizonServer;

    private Consumer<TransactionResponse> transactionResponseConsumer;

    public TransactionListenerConfiguration (IngestionApiClient apiClient,
                                             ModelMapper modelMapper,
                                             Server horizonServer) {
        this.apiClient = apiClient;
        this.modelMapper = modelMapper;
        this.horizonServer = horizonServer;
    }

    @PostConstruct
    public void createPipeline () {
        Flux.<TransactionResponse>create(sink -> registerListener(sink::next))
            .map(transactionResponse -> {
                List<OperationResponse> operationResponses = fetchOperationsForTransaction(transactionResponse);
                processAccounts(operationResponses);
                return modelMapper.map(transactionResponse, operationResponses);
            })
            .map(Mono::just)
            .subscribe(
                transactionMono -> LOG.info("API responded with object {}", transactionMono.block()),
                Throwable::printStackTrace
            );
    }

    private void processAccounts (List<OperationResponse> operationResponses) {
        Flux.fromIterable(operationResponses)
            .filter(operationResponse -> operationResponse.getClass() == CreateAccountOperationResponse.class)
            .map(operationResponse -> modelMapper.mapNewContract((CreateAccountOperationResponse) operationResponse))
            .subscribe(
                address -> LOG.info("API responded with object {}", address),
                Throwable::printStackTrace
            );
    }

    @PostConstruct
    public void subscribeOnTransactions () {
        horizonServer
            .transactions()
            .cursor("now")
            .stream(transactionResponse -> transactionResponseConsumer.accept(transactionResponse));
    }

    private List<OperationResponse> fetchOperationsForTransaction (TransactionResponse transactionResponse) {
        try {
            return horizonServer
                .operations()
                .forTransaction(transactionResponse.getHash())
                .execute()
                .getRecords();
        }
        catch (IOException ex) {
            return Collections.emptyList();
        }
    }

    private void registerListener (Consumer<TransactionResponse> transactionResponseConsumer) {
        this.transactionResponseConsumer = transactionResponseConsumer;
    }
}
