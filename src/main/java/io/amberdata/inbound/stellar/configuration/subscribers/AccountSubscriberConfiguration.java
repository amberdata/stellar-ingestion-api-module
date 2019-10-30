package io.amberdata.inbound.stellar.configuration.subscribers;

import io.amberdata.inbound.core.client.BlockchainEntityWithState;
import io.amberdata.inbound.core.client.InboundApiClient;
import io.amberdata.inbound.core.state.ResourceStateStorage;
import io.amberdata.inbound.domain.Address;
import io.amberdata.inbound.stellar.client.HorizonServer;
import io.amberdata.inbound.stellar.configuration.history.HistoricalManager;
import io.amberdata.inbound.stellar.configuration.properties.BatchSettings;
import io.amberdata.inbound.stellar.mapper.ModelMapper;

import java.io.IOException;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import org.stellar.sdk.FormatException;
import org.stellar.sdk.requests.EventListener;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.TransactionResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import shadow.com.google.common.base.Optional;

@Configuration
@ConditionalOnProperty(prefix = "stellar", name = "subscribe-on-accounts")
public class AccountSubscriberConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(AccountSubscriberConfiguration.class);

  private final ResourceStateStorage    stateStorage;
  private final InboundApiClient        apiClient;
  private final ModelMapper             modelMapper;
  private final HistoricalManager       historicalManager;
  private final HorizonServer           server;
  private final BatchSettings           batchSettings;
  private final SubscriberErrorsHandler errorsHandler;

  public AccountSubscriberConfiguration(
      ResourceStateStorage    stateStorage,
      InboundApiClient        apiClient,
      ModelMapper             modelMapper,
      HistoricalManager       historicalManager,
      HorizonServer           server,
      BatchSettings           batchSettings,
      SubscriberErrorsHandler errorsHandler
  ) {
    this.stateStorage      = stateStorage;
    this.apiClient         = apiClient;
    this.modelMapper       = modelMapper;
    this.historicalManager = historicalManager;
    this.server            = server;
    this.batchSettings     = batchSettings;
    this.errorsHandler     = errorsHandler;
  }

  @PostConstruct
  public void createPipeline() {
    LOG.info("Going to subscribe on Stellar Accounts stream through Transactions stream");

    Flux
        .<TransactionResponse>create(
          sink -> subscribe(
            sink::next,
            SubscriberErrorsHandler::handleFatalApplicationError
          )
        )
        .publishOn(Schedulers.newElastic("addresses-subscriber-thread"))
        .timeout(this.errorsHandler.timeoutDuration())
        .map(this::toAddressesStream)
        .flatMap(Flux::fromStream)
        .buffer(this.batchSettings.addressesInChunk())
        .retryWhen(errorsHandler::onError)
        .subscribe(
            entities -> this.apiClient.publishWithState("/addresses", entities),
            SubscriberErrorsHandler::handleFatalApplicationError
      );
  }

  private Stream<BlockchainEntityWithState<Address>> toAddressesStream(
      TransactionResponse transactionResponse
  ) {
    List<OperationResponse> operationResponses = fetchOperationsForTransaction(transactionResponse);
    return processAccounts(operationResponses, transactionResponse);
  }

  private Stream<BlockchainEntityWithState<Address>> processAccounts(
      List<OperationResponse> operationResponses,
      TransactionResponse     transactionResponse
  ) {
    return this.modelMapper.mapOperations(operationResponses, null)
        .stream()
        .flatMap(
            functionCall -> {
              Stream.Builder<BlockchainEntityWithState<Address>> stream = Stream.builder();
              if (functionCall.getFrom() != null) {
                AccountResponse accountResponse = this.fetchAccountDetails(functionCall.getFrom());
                if (accountResponse != null) {
                  stream.add(
                      this.modelMapper.mapAccountWithState(
                        accountResponse,
                        functionCall.getTimestamp(),
                        transactionResponse.getPagingToken()
                      )
                  );
                }
              }
              if (functionCall.getTo() != null) {
                AccountResponse accountResponse = this.fetchAccountDetails(functionCall.getTo());
                if (accountResponse != null) {
                  stream.add(
                      this.modelMapper.mapAccountWithState(
                        accountResponse,
                        functionCall.getTimestamp(),
                        transactionResponse.getPagingToken()
                      )
                  );
                }
              }
              return stream.build();
            }
        )
        .distinct();
  }

  private void subscribe(Consumer<TransactionResponse> responseConsumer,
                         Consumer<? super Throwable>   errorConsumer) {
    String cursorPointer = this.getCursorPointer();

    LOG.info("Subscribing to addresses using transactions cursor {}", cursorPointer);

    this.server.testConnection();
    this.testCursorCorrectness(cursorPointer);

    this.server.horizonServer()
        .transactions()
        .cursor(cursorPointer)
        .stream(new EventListener<TransactionResponse>() {
          @Override
          public void onEvent(TransactionResponse transactionResponse) {
            responseConsumer.accept(transactionResponse);
          }

          @Override
          public void onFailure(Optional<Throwable> optional, Optional<Integer> optional1) {
            if (optional.isPresent()) {
              errorConsumer.accept(optional.get());
            }
          }
        });
  }

  private String getCursorPointer() {
    if (this.historicalManager.disabled()) {
      return this.stateStorage.getStateToken(Address.class.getSimpleName(), () -> "now");
    } else {
      return this.historicalManager.transactionPagingToken();
    }
  }

  private List<OperationResponse> fetchOperationsForTransaction(
      TransactionResponse transactionResponse
  ) {
    try {
      return this.server.horizonServer()
          .operations()
          .forTransaction(transactionResponse.getHash())
          .execute()
          .getRecords();
    } catch (IOException | FormatException e) {
      LOG.error(
          "Unable to fetch information about operations for transaction "
          + transactionResponse.getHash(),
          e
      );
      return Collections.emptyList();
    }
  }

  private AccountResponse fetchAccountDetails(String accountId) {
    try {
      return this.server.horizonServer()
          .accounts()
          .account(accountId);
    } catch (Exception e) {
      LOG.error("Unable to get details for account " + accountId, e);
      return null;
    }
  }

  private void testCursorCorrectness(String cursorPointer) {
    try {
      this.server.horizonServer().transactions().cursor(cursorPointer).limit(1).execute();
    } catch (IOException ioe) {
      throw new HorizonServer.IncorrectRequestException(
          "Failed to test if cursor value is valid",
          ioe
      );
    }
  }

}
