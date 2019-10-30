package io.amberdata.inbound.stellar.configuration.subscribers;

import io.amberdata.inbound.core.client.BlockchainEntityWithState;
import io.amberdata.inbound.core.client.InboundApiClient;
import io.amberdata.inbound.core.state.ResourceStateStorage;
import io.amberdata.inbound.domain.Address;
import io.amberdata.inbound.domain.Asset;
import io.amberdata.inbound.domain.Block;
import io.amberdata.inbound.domain.FunctionCall;
import io.amberdata.inbound.domain.Transaction;
import io.amberdata.inbound.stellar.client.HorizonServer;
import io.amberdata.inbound.stellar.configuration.history.HistoricalManager;
import io.amberdata.inbound.stellar.configuration.properties.BatchSettings;
import io.amberdata.inbound.stellar.mapper.ModelMapper;

import java.io.IOException;

import java.time.Duration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import org.stellar.sdk.FormatException;
import org.stellar.sdk.requests.EventListener;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.AssetResponse;
import org.stellar.sdk.responses.LedgerResponse;
import org.stellar.sdk.responses.TransactionResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import shadow.com.google.common.base.Optional;

@Configuration
@ConditionalOnProperty(prefix = "stellar", name = "subscribe-on-all")
public class StellarSubscriberConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(StellarSubscriberConfiguration.class);

  private final ResourceStateStorage    stateStorage;
  private final InboundApiClient        apiClient;
  private final ModelMapper             modelMapper;
  private final HistoricalManager       historicalManager;
  private final HorizonServer           server;
  private final BatchSettings           batchSettings;
  private final SubscriberErrorsHandler errorsHandler;
  private final Cache<String, Asset>    cache;

  public StellarSubscriberConfiguration(
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

    CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build();
    cacheManager.init();

    this.cache = cacheManager.createCache(
        "assets",
        CacheConfigurationBuilder
            .newCacheConfigurationBuilder(
                String.class,
                Asset.class,
                ResourcePoolsBuilder.heap(10000)
            )
            .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofHours(24)))
    );
  }

  @PostConstruct
  public void createPipeline() {
    LOG.info("Going to subscribe on Stellar Ledgers stream");

    Flux
        .<LedgerResponse>push(
            sink -> this.subscribe(
              sink::next,
              SubscriberErrorsHandler::handleFatalApplicationError)
        )
        .publishOn(Schedulers.newElastic("ledgers-subscriber-thread"))
        .timeout(this.errorsHandler.timeoutDuration())
        .map(this.modelMapper::mapLedgerWithState)
        .buffer(this.batchSettings.blocksInChunk())
        .retryWhen(errorsHandler::onError)
        .subscribe(
            this::processLedgers,
            SubscriberErrorsHandler::handleFatalApplicationError
      );
  }

  private void subscribe(Consumer<LedgerResponse>    responseConsumer,
                         Consumer<? super Throwable> errorConsumer) {
    String cursorPointer = this.getCursorPointer();

    LOG.info("Subscribing to ledgers using cursor {}", cursorPointer);

    this.server.testConnection();
    this.testCursorCorrectness(cursorPointer);

    this.server.horizonServer()
        .ledgers()
        .cursor(cursorPointer)
        .stream(new EventListener<LedgerResponse>() {
          @Override
          public void onEvent(LedgerResponse ledgerResponse) {
            responseConsumer.accept(ledgerResponse);
          }

          @Override
          public void onFailure(Optional<Throwable> optional, Optional<Integer> optional1) {
            if (optional.isPresent()) {
              errorConsumer.accept(optional.get());
            }
          }
        });
  }

  private void processLedgers(List<BlockchainEntityWithState<Block>> blocks) {
    long timeLedgers = System.currentTimeMillis();

    for (BlockchainEntityWithState<Block> block : blocks) {
      long timeLedger = System.currentTimeMillis();

      long ledger = block.getEntity().getNumber().longValue();

      List<Transaction> transactions = new ArrayList<>();
      Set<Address>      addresses    = new HashSet<>();
      Set<Asset>        assets       = new HashSet<>();

      try {
        long timeTransactions = System.currentTimeMillis();
        List<TransactionResponse> transactionResponses = this.server.horizonServer()
            .transactions()
            .forLedger(ledger)
            .execute()
            .getRecords();
        LOG.info("[PERFORMANCE] getTransactions (" + transactionResponses.size() + "): " + (System.currentTimeMillis() - timeTransactions) + " ms");

        long timeOperations = System.currentTimeMillis();
        List<OperationResponse> operationResponses = this.fetchOperationsForLedger(ledger);
        LOG.info("[PERFORMANCE] getOperations (" + operationResponses.size() + "): " + (System.currentTimeMillis() - timeOperations) + " ms");

        Map<String, List<OperationResponse>> operations = new HashMap<>();
        for (OperationResponse operationResponse : operationResponses) {
          String transactionHash = operationResponse.getTransactionHash();
          List<OperationResponse> ops = operations.computeIfAbsent(transactionHash, k -> new ArrayList<>());
          ops.add(operationResponse);
        }

        for (TransactionResponse transactionResponse : transactionResponses) {
          Transaction transaction = this.enrichTransaction(transactionResponse, operations.getOrDefault(transactionResponse.getHash(), Collections.emptyList()));
          transactions.add(transaction);

          long timeAddresses = System.currentTimeMillis();
          addresses.addAll(this.collectAddresses(transaction.getFunctionCalls()));
          LOG.info("[PERFORMANCE] getAddresses: " + (System.currentTimeMillis() - timeAddresses) + " ms");

          long timeAssets = System.currentTimeMillis();
          assets.addAll(this.collectAssets(operationResponses, ledger));
          LOG.info("[PERFORMANCE] getAssets: " + (System.currentTimeMillis() - timeAssets) + " ms");
        }
      } catch (IOException ioe) {
        LOG.error("Unable to fetch information about transactions for ledger " + ledger, ioe);
      }

      if (!addresses.isEmpty()) {
        long timePublishAddresses = System.currentTimeMillis();
        this.apiClient.publish("/addresses", new ArrayList<>(addresses));
        LOG.info("[PERFORMANCE] publishAddresses (" + addresses.size() + "): " + (System.currentTimeMillis() - timePublishAddresses) + " ms");
      }

      if (!assets.isEmpty()) {
        long timePublishAssets = System.currentTimeMillis();
        this.apiClient.publish("/assets", new ArrayList<>(assets));
        LOG.info("[PERFORMANCE] publishAssets (" + assets.size() + "): " + (System.currentTimeMillis() - timePublishAssets) + " ms");
      }

      if (!transactions.isEmpty()) {
        long timePublishTransactions = System.currentTimeMillis();
        this.apiClient.publish("/transactions", transactions);
        LOG.info("[PERFORMANCE] publishTransactions (" + transactions.size() + "): " + (System.currentTimeMillis() - timePublishTransactions) + " ms");
      }

      LOG.info("[PERFORMANCE] ledger: " + (System.currentTimeMillis() - timeLedger) + " ms");
    }

    long timePublishLedgers = System.currentTimeMillis();
    this.apiClient.publishWithState("/blocks", blocks);
    LOG.info("[PERFORMANCE] publishLedgers (" + blocks.size() + "): " + (System.currentTimeMillis() - timePublishLedgers) + " ms");

    LOG.info("[PERFORMANCE] ledgers: " + (System.currentTimeMillis() - timeLedgers) + " ms");
  }

  private Transaction enrichTransaction(
      TransactionResponse     transactionResponse,
      List<OperationResponse> operationResponses
  ) {
    return this.modelMapper.mapTransaction(
        transactionResponse,
        operationResponses
    );
  }

  private List<OperationResponse> fetchOperationsForLedger(long ledger) {
    try {
      return this.server.horizonServer()
          .operations()
          .forLedger(ledger)
          .execute()
          .getRecords();
    } catch (IOException | FormatException e) {
      LOG.error(
          "Unable to fetch information about operations for ledger " + ledger,
          e
      );
      return Collections.emptyList();
    }
  }

  private Collection<Address> collectAddresses(List<FunctionCall> functionCalls) {
    Map<String, Address> addresses = new HashMap<>();

    // TODO: report account balances properly to the inbound API

    for (FunctionCall functionCall : functionCalls) {
      if (
          functionCall.getFrom() != null
          && !addresses.containsKey(functionCall.getFrom())
      ) {
        AccountResponse accountResponse = this.fetchAccountDetails(functionCall.getFrom());
        if (accountResponse != null) {
          addresses.put(
              functionCall.getFrom(),
              this.modelMapper.mapAccount(accountResponse, functionCall.getTimestamp())
          );
        }
      }
      if (
          functionCall.getTo() != null
          && !addresses.containsKey(functionCall.getTo())
      ) {
        AccountResponse accountResponse = this.fetchAccountDetails(functionCall.getTo());
        if (accountResponse != null) {
          addresses.put(
              functionCall.getTo(),
              this.modelMapper.mapAccount(accountResponse, functionCall.getTimestamp())
          );
        }
      }
    }

    return addresses.values();
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

  private List<Asset> collectAssets(List<OperationResponse> operationResponses, Long ledger) {
    return this.modelMapper.mapAssets(operationResponses, ledger).stream()
        .distinct()
        .map(this::enrichAsset)
        .collect(Collectors.toList());
  }

  private Asset enrichAsset(Asset asset) {
    String assetHash = asset.getType() + "_" + asset.getCode() + "_" + asset.getIssuerAccount();
    if (this.cache.containsKey(assetHash)) {
      return this.cache.get(assetHash);
    }

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
        asset.setAmount(assetResponse.getAmount());
        asset.setMeta(assetOptionalProperties(assetResponse));
      }
    } catch (Exception e) {
      LOG.error("Error during fetching an asset: " + asset.getCode(), e);
    }

    if (asset.getAmount() == null || !this.isNumeric(asset.getAmount())) {
      asset.setAmount("0");
    }

    this.cache.put(assetHash, asset);

    return asset;
  }

  private Map<String, Object> assetOptionalProperties(AssetResponse assetResponse) {
    Map<String, Object> optionalProperties = new HashMap<>();

    optionalProperties.put("num_accounts",        assetResponse.getNumAccounts());
    optionalProperties.put("flag_auth_required",  assetResponse.getFlags().isAuthRequired());
    optionalProperties.put("flag_auth_revocable", assetResponse.getFlags().isAuthRevocable());

    return optionalProperties;
  }

  private boolean isNumeric(String string) {
    return string.matches("\\d+(\\.\\d+)?");
  }

  private String getCursorPointer() {
    if (this.historicalManager.disabled()) {
      return this.stateStorage.getStateToken(Block.class.getSimpleName(), () -> "now");
    } else {
      return this.historicalManager.ledgerPagingToken();
    }
  }

  private void testCursorCorrectness(String cursorPointer) {
    try {
      this.server.horizonServer().ledgers().cursor(cursorPointer).limit(1).execute();
    } catch (IOException ioe) {
      throw new HorizonServer.IncorrectRequestException(
          "Failed to test if cursor value is valid",
          ioe
      );
    }
  }

}
