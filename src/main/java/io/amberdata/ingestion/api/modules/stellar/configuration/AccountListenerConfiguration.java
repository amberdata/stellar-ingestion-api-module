package io.amberdata.ingestion.api.modules.stellar.configuration;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Server;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.TransactionResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.ingestion.api.modules.stellar.client.IngestionApiClient;
import io.amberdata.ingestion.api.modules.stellar.mapper.ModelMapper;

@Configuration
public class AccountListenerConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(AccountListenerConfiguration.class);

    private final IngestionApiClient apiClient;
    private final ModelMapper        modelMapper;
    private final Server             horizonServer;

    private Consumer<TransactionResponse> transactionResponseConsumer;

    public AccountListenerConfiguration (IngestionApiClient apiClient,
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
                return processAccounts(operationResponses).stream()
                    .map(modelMapper::map);
            })
            .map(Mono::just)
            .subscribe(
                addressMono -> LOG.info("API responded with object {}", addressMono.block()),
                Throwable::printStackTrace
            );
    }

    private List<AccountResponse> processAccounts (List<OperationResponse> operationResponses) {
        return modelMapper.map(operationResponses).stream()
            .flatMap(e -> {
                Stream.Builder<String> stream = Stream.builder();
                if (e.getFrom() != null) {
                    stream.add(e.getFrom());
                }
                if (e.getTo() != null) {
                    stream.add(e.getTo());
                }
                return stream.build();
            })
            .distinct()
            .map(this::fetchAccountDetails)
            .collect(Collectors.toList());
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

    private AccountResponse fetchAccountDetails (String accountId) {
        try {
            return horizonServer
                .accounts()
                .account(KeyPair.fromAccountId(accountId));
        }
        catch (IOException ex) {
            return null;
        }
    }

    private void registerListener (Consumer<TransactionResponse> transactionResponseConsumer) {
        this.transactionResponseConsumer = transactionResponseConsumer;
    }
}
