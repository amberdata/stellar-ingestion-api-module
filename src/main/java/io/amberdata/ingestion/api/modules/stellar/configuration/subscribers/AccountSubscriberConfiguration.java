package io.amberdata.ingestion.api.modules.stellar.configuration.subscribers;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.TransactionResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.domain.Address;
import io.amberdata.ingestion.api.modules.stellar.client.HorizonServer;
import io.amberdata.ingestion.api.modules.stellar.client.IngestionApiClient;
import io.amberdata.ingestion.api.modules.stellar.mapper.ModelMapper;
import io.amberdata.ingestion.api.modules.stellar.state.ResourceStateStorage;
import io.amberdata.ingestion.api.modules.stellar.state.entities.Resource;

import javax.annotation.PostConstruct;
import reactor.core.publisher.Flux;

@Configuration
@ConditionalOnProperty(prefix = "stellar", name="subscribe-on-accounts")
public class AccountSubscriberConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(AccountSubscriberConfiguration.class);

    private final ResourceStateStorage stateStorage;
    private final IngestionApiClient   apiClient;
    private final ModelMapper          modelMapper;
    private final HorizonServer        server;

    public AccountSubscriberConfiguration (ResourceStateStorage stateStorage,
                                           IngestionApiClient apiClient,
                                           ModelMapper modelMapper,
                                           HorizonServer server) {

        this.stateStorage = stateStorage;
        this.apiClient = apiClient;
        this.modelMapper = modelMapper;
        this.server = server;
    }

    @PostConstruct
    public void createPipeline () {
        LOG.info("Going to subscribe on Stellar Accounts stream through Transactions stream");

        Flux.<TransactionResponse>create(sink -> subscribe(sink::next))
            .map(transactionResponse -> {
                List<OperationResponse> operationResponses = fetchOperationsForTransaction(transactionResponse);
                return processAccounts(operationResponses).stream()
                    .map(accountResponse -> modelMapper.map(accountResponse, transactionResponse.getPagingToken()))
                    .collect(Collectors.toList());
            })
            .map(entities -> apiClient.publish("/addresses", entities, Address.class))
            .subscribe(stateStorage::storeState, SubscriberErrorsHandler::handleFatalApplicationError);
    }

    private List<AccountResponse> processAccounts (List<OperationResponse> operationResponses) {
        return modelMapper.map(operationResponses, null).stream()
            .flatMap(functionCall ->
                Stream.of(
                    Optional.ofNullable(functionCall.getFrom()),
                    Optional.ofNullable(functionCall.getTo())
                )
            )
            .filter(Optional::isPresent)
            .map(Optional::get)
            .distinct()
            .map(this::fetchAccountDetails)
            .collect(Collectors.toList());
    }

    private void subscribe (Consumer<TransactionResponse> stellarSdkResponseConsumer) {
        String cursorPointer = stateStorage.getCursorPointer(Resource.ACCOUNT);

        LOG.info("Addresses cursor is set to {} [using transactions cursor]", cursorPointer);

        server.testConnection();
        testCursorCorrectness(cursorPointer);

        server.horizonServer()
            .transactions()
            .cursor(cursorPointer)
            .stream(stellarSdkResponseConsumer::accept);
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

    private AccountResponse fetchAccountDetails (String accountId) {
        try {
            return server.horizonServer()
                .accounts()
                .account(KeyPair.fromAccountId(accountId));
        }
        catch (IOException ex) {
            return null;
        }
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
