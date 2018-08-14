package io.amberdata.ingestion.stellar.configuration.subscribers;

import io.amberdata.ingestion.core.client.BlockchainEntityWithState;
import io.amberdata.ingestion.core.client.IngestionApiClient;
import io.amberdata.ingestion.core.state.ResourceStateStorage;
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
import org.stellar.sdk.FormatException;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.TransactionResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.ingestion.domain.Address;
import io.amberdata.ingestion.stellar.client.HorizonServer;
import io.amberdata.ingestion.stellar.mapper.ModelMapper;
import io.amberdata.domain.Address;
import io.amberdata.ingestion.api.modules.stellar.client.HorizonServer;
import io.amberdata.ingestion.api.modules.stellar.client.IngestionApiClient;
import io.amberdata.ingestion.api.modules.stellar.mapper.ModelMapper;
import io.amberdata.ingestion.api.modules.stellar.state.ResourceStateStorage;
import io.amberdata.ingestion.api.modules.stellar.state.entities.BlockchainEntityWithState;
import io.amberdata.ingestion.api.modules.stellar.state.entities.Resource;
import io.amberdata.ingestion.api.modules.stellar.util.PreAuthTransactionProcessor;

import javax.annotation.PostConstruct;
import reactor.core.publisher.Flux;

@Configuration
@ConditionalOnProperty(prefix = "stellar", name="subscribe-on-accounts")
public class AccountSubscriberConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(AccountSubscriberConfiguration.class);

    private final ResourceStateStorage        stateStorage;
    private final IngestionApiClient          apiClient;
    private final ModelMapper                 modelMapper;
    private final HorizonServer               server;
    private final PreAuthTransactionProcessor preAuthTransactionProcessor;

    public AccountSubscriberConfiguration (ResourceStateStorage stateStorage,
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
        LOG.info("Going to subscribe on Stellar Accounts stream through Transactions stream");

        Flux.<TransactionResponse>create(sink -> subscribe(sink::next))
            .map(transactionResponse -> {
                List<OperationResponse> operationResponses = fetchOperationsForTransaction(transactionResponse);
                return processAccounts(operationResponses, transactionResponse).collect(Collectors.toList());
            })
            .map(entities -> apiClient.publish("/addresses", entities, Address.class))
            .subscribe(null, SubscriberErrorsHandler::handleFatalApplicationError);
    }

    private Stream<BlockchainEntityWithState<Address>> processAccounts (List<OperationResponse> operationResponses,
                                                                        TransactionResponse transactionResponse) {
        return modelMapper.map(operationResponses, null).stream()
            .flatMap(functionCall ->
                Stream.of(
                    Optional.ofNullable(
                        functionCall.getFrom() != null ?
                            AccountWithTime.from(functionCall.getFrom(), functionCall.getTimestamp()) : null),
                    Optional.ofNullable(
                        functionCall.getTo() != null ?
                            AccountWithTime.from(functionCall.getTo(), functionCall.getTimestamp()) : null)
                )
            )
            .filter(Optional::isPresent)
            .map(Optional::get)
            .distinct()
            .map(accountWithTime -> {
                AccountResponse accountResponse = this.fetchAccountDetails(accountWithTime.getName());
                return modelMapper.map(
                    accountResponse,
                    transactionResponse.getPagingToken(),
                    accountWithTime.getTimestamp()
                );
            });
    }

    private void subscribe (Consumer<TransactionResponse> stellarSdkResponseConsumer) {
        String cursorPointer = stateStorage.getStateToken(Address.class.getSimpleName());

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
        catch (FormatException ex) {
            return this.preAuthTransactionProcessor.fetchOperations(transactionResponse.getHash());
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

    public static class AccountWithTime {

        private String name;
        private Long timestamp;

        private AccountWithTime (String name, Long timestamp) {
            this.name = name;
            this.timestamp = timestamp;
        }

        public static AccountWithTime from (String name, Long timestamp) {
            return new AccountWithTime(name, timestamp);
        }

        public String getName () {
            return name;
        }

        public Long getTimestamp () {
            return timestamp;
        }
    }
}
