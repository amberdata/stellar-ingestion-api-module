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
import io.amberdata.ingestion.stellar.configuration.history.HistoricalManager;
import io.amberdata.ingestion.stellar.configuration.properties.BatchSettings;
import io.amberdata.ingestion.stellar.mapper.ModelMapper;

import javax.annotation.PostConstruct;
import reactor.core.publisher.Flux;

@Configuration
@ConditionalOnProperty(prefix = "stellar", name="subscribe-on-accounts")
public class AccountSubscriberConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(AccountSubscriberConfiguration.class);

    private final ResourceStateStorage stateStorage;
    private final IngestionApiClient   apiClient;
    private final ModelMapper          modelMapper;
    private final HistoricalManager    historicalManager;
    private final HorizonServer        server;
    private final BatchSettings        batchSettings;

    public AccountSubscriberConfiguration (ResourceStateStorage stateStorage,
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
        LOG.info("Going to subscribe on Stellar Accounts stream through Transactions stream");

        Flux.<TransactionResponse>create(sink -> subscribe(sink::next))
            .map(transactionResponse -> {
                List<OperationResponse> operationResponses = fetchOperationsForTransaction(transactionResponse);
                return processAccounts(operationResponses, transactionResponse);
            })
            .flatMap(Flux::fromStream)
            .buffer(this.batchSettings.addressesInChunk())
            .retryWhen(SubscriberErrorsHandler::onError)
            .map(entities -> this.apiClient.publish("/addresses", entities))
            .timestamp()
            .subscribe(
                (tuple -> LOG.info("Published address at {}", tuple.getT1())),
                SubscriberErrorsHandler::handleFatalApplicationError
            );
    }

    private Stream<BlockchainEntityWithState<Address>> processAccounts (List<OperationResponse> operationResponses,
                                                                        TransactionResponse transactionResponse) {
        return this.modelMapper.map(operationResponses, null).stream()
            .flatMap(functionCall -> {
                Stream.Builder<BlockchainEntityWithState<Address>> stream = Stream.builder();
                if (functionCall.getFrom() != null) {
                    AccountResponse accountResponse = this.fetchAccountDetails(functionCall.getFrom());
                    if (accountResponse != null) {
                        stream.add(this.modelMapper.map(
                            accountResponse,
                            transactionResponse.getPagingToken(),
                            functionCall.getTimestamp()
                        ));
                    }
                }
                if (functionCall.getTo() != null) {
                    AccountResponse accountResponse = this.fetchAccountDetails(functionCall.getTo());
                    if (accountResponse != null) {
                        stream.add(this.modelMapper.map(
                            accountResponse,
                            transactionResponse.getPagingToken(),
                            functionCall.getTimestamp()
                        ));
                    }
                }
                return stream.build();
            })
            .distinct();
    }

    private void subscribe (Consumer<TransactionResponse> stellarSdkResponseConsumer) {
        String cursorPointer = getCursorPointer();

        LOG.info("Addresses cursor is set to {} [using transactions cursor]", cursorPointer);

        this.server.testConnection();
        testCursorCorrectness(cursorPointer);

        this.server.horizonServer()
            .transactions()
            .cursor(cursorPointer)
            .stream(stellarSdkResponseConsumer::accept);
    }

    private String getCursorPointer () {
        if (this.historicalManager.disabled()) {
            return this.stateStorage.getStateToken(Address.class.getSimpleName(), () -> "now");
        } else {
            return this.historicalManager.transactionPagingToken();
        }
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

    private AccountResponse fetchAccountDetails (String accountId) {
        try {
            return this.server.horizonServer()
                .accounts()
                .account(KeyPair.fromAccountId(accountId));
        }
        catch (Exception ex) {
            LOG.error("Unable to get details for account {}", accountId);
            return null;
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
