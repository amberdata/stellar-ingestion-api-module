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
import io.amberdata.inbound.stellar.client.Metrics;
import io.amberdata.inbound.stellar.configuration.history.HistoricalManager;
import io.amberdata.inbound.stellar.configuration.properties.BatchSettings;
import io.amberdata.inbound.stellar.mapper.ModelMapper;

import java.io.IOException;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
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
import org.stellar.sdk.requests.AssetsRequestBuilder;
import org.stellar.sdk.requests.EventListener;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.AssetResponse;
import org.stellar.sdk.responses.LedgerResponse;
import org.stellar.sdk.responses.Page;
import org.stellar.sdk.responses.TransactionResponse;
import org.stellar.sdk.responses.effects.EffectResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import shadow.com.google.common.base.Optional;

@Configuration
@ConditionalOnProperty(prefix = "stellar", name = "subscribe-on-all")
public class StellarSubscriberConfiguration {
  private static final Logger LOG = LoggerFactory.getLogger(StellarSubscriberConfiguration.class);

  /* package */ static void subscribeToLedgers(HorizonServer               server,
                                               String                      cursorPointer,
                                               Consumer<LedgerResponse>    responseConsumer,
                                               Consumer<? super Throwable> errorConsumer) {
    LOG.info("Subscribing to ledgers using cursor {}", cursorPointer);

    server.testConnection();
    StellarSubscriberConfiguration.testCursorCorrectness(server, cursorPointer);

    // LedgerResponse ledgerResponse = this.server.horizonServer().ledgers().ledger(0L);

    server.horizonServer()
        .ledgers()
        .cursor(cursorPointer)
        .limit(HorizonServer.HORIZON_PER_REQUEST_LIMIT)
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

  /* package */ static Asset enrichAsset(HorizonServer server, Asset asset) {
    try {
      final AssetsRequestBuilder builder = server
              .horizonServer()
              .assets()
              .assetCode(asset.getCode())
              .assetIssuer(asset.getIssuerAccount());

      // limit() modifies the builder in-place. OK to discard the return value here.
      builder.limit(HorizonServer.HORIZON_PER_REQUEST_LIMIT);

      List<AssetResponse> records = StellarSubscriberConfiguration.getObjects(
          server,
          builder.execute(),
          "assets"
      );

      if (records.size() > 0) {
        AssetResponse assetResponse = records.get(0);
        asset.setType(Asset.AssetType.fromName(assetResponse.getAssetType()));
        asset.setCode(assetResponse.getAssetCode());
        asset.setIssuerAccount(assetResponse.getAssetIssuer());
        asset.setAmount(assetResponse.getAmount());
        asset.setMeta(StellarSubscriberConfiguration.assetOptionalProperties(assetResponse));
      }
    } catch (Exception e) {
      LOG.error("Error during fetching an asset: " + asset.getCode(), e);
      Metrics.count("assets.errors.fetch", 1);
    }

    if (asset.getAmount() == null || !StellarSubscriberConfiguration.isNumeric(asset.getAmount())) {
      asset.setAmount("0");
      Metrics.count("assets.errors.zeroamount", 1);
    }

    return asset;
  }

  /* package */ static <T> List<T> getObjects(
      final HorizonServer server,
      Page<T> page,
      final String metricsKey
  ) {
    List<T> list     = new ArrayList<>();
    String  previous = null;
    String  current  = page.getLinks().getSelf().getHref();

    try {
      do {
        if ((current == null) || current.equals(previous)) {
          break;
        }

        if (!metricsKey.isEmpty()) {
          Metrics.count(metricsKey, page.getRecords().size());
        }
        list.addAll(page.getRecords());
        page = page.getNextPage(server.horizonServer().getHttpClient());

        previous = current;
        current  = page == null ? null : page.getLinks().getSelf().getHref();
      } while (page != null);
    } catch (IOException | URISyntaxException e) {
      throw new HorizonServer.StellarException(e.getMessage(), e.getCause());
    }

    return list;
  }

  private static Map<String, Object> assetOptionalProperties(AssetResponse assetResponse) {
    Map<String, Object> optionalProperties = new HashMap<>();

    optionalProperties.put("num_accounts",        assetResponse.getNumAccounts());
    optionalProperties.put("flag_auth_required",  assetResponse.getFlags().isAuthRequired());
    optionalProperties.put("flag_auth_revocable", assetResponse.getFlags().isAuthRevocable());

    return optionalProperties;
  }

  private static boolean isNumeric(String string) {
    return string.matches("\\d+(\\.\\d+)?");
  }

  private static void testCursorCorrectness(HorizonServer server, String cursorPointer) {
    try {
      server.horizonServer().ledgers().cursor(cursorPointer).limit(1).execute();
    } catch (IOException ioe) {
      throw new HorizonServer.IncorrectRequestException(
          "Failed to test if cursor value is valid",
          ioe
      );
    }
  }

  private final ResourceStateStorage    stateStorage;
  private final InboundApiClient        apiClient;
  private final ModelMapper             modelMapper;
  private final HistoricalManager       historicalManager;
  private final HorizonServer           server;
  private final BatchSettings           batchSettings;
  private final SubscriberErrorsHandler errorsHandler;
  private final Cache<String, Asset>    cache;

  /**
   * Default constructor.
   *
   * @param stateStorage      the state storage
   * @param apiClient         the client api
   * @param modelMapper       the model mapper
   * @param historicalManager the historical manager
   * @param server            the Horizon server
   * @param batchSettings     the batch settings
   * @param errorsHandler     the error handler
   */
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

  /**
   * Creates the global pipeline.
   */
  @PostConstruct
  public void createPipeline() {
    LOG.info("Going to subscribe on Stellar Ledgers stream");

    Flux
        .<LedgerResponse>push(
          sink -> StellarSubscriberConfiguration.subscribeToLedgers(
            this.server,
            this.getCursorPointer(),
            sink::next,
            SubscriberErrorsHandler::handleFatalApplicationError
          )
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

  private void processLedgers(List<BlockchainEntityWithState<Block>> blocks) {
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    long timeLedgers = System.currentTimeMillis();
    long maxSequence = 0;

    for (BlockchainEntityWithState<Block> block : blocks) {
      Metrics.count("blocks", 1);

      @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
      long timeLedger = System.currentTimeMillis();

      long ledger = block.getEntity().getNumber().longValue();
      if (ledger > maxSequence) {
        maxSequence = ledger;
      }

      List<Transaction> transactions = new ArrayList<>();
      Set<Address>      addresses    = new HashSet<>();
      Set<Asset>        assets       = new HashSet<>();

      try {
        long timeTransactions = System.currentTimeMillis();
        final List<TransactionResponse> responses = StellarSubscriberConfiguration.getObjects(
            this.server,
            this.server.horizonServer()
              .transactions()
              .forLedger(ledger)
              .limit(HorizonServer.HORIZON_PER_REQUEST_LIMIT)
              .execute(),
            "ledger.transactions"
        );
        this.logPerformance("getTransactions", timeTransactions);

        final long timeEffects = System.currentTimeMillis();
        final Map<Long, String> effectLookup = this.fetchEffectsForLedger(ledger);
        this.logPerformance("getEffects", timeEffects);

        final long timeOperations = System.currentTimeMillis();
        final List<OperationResponse> operationResponses = this.fetchOperationsForLedger(ledger);
        this.logPerformance("getOperations", timeOperations);

        final Map<String, List<OperationResponse>> operations = new HashMap<>();
        for (final OperationResponse operationResponse : operationResponses) {
          String transactionHash = operationResponse.getTransactionHash();
          operations
              .computeIfAbsent(transactionHash, k -> new ArrayList<>())
              .add(operationResponse);
        }

        final long timeEnrichTransactions = System.currentTimeMillis();
        for (final TransactionResponse transactionResponse : responses) {
          final Transaction transaction = this.enrichTransaction(
              transactionResponse,
              effectLookup,
              operations.getOrDefault(transactionResponse.getHash(), Collections.emptyList())
          );
          transactions.add(transaction);
          addresses.addAll(this.collectAddresses(transaction.getFunctionCalls()));
        }
        assets.addAll(this.collectAssets(operationResponses, ledger));
        this.logPerformance("enrichTransactions", timeEnrichTransactions);
      } catch (IOException ioe) {
        LOG.error("Unable to fetch information about transactions for ledger " + ledger, ioe);
        Metrics.count("ledger.errors", 1);
      }

      final long timePublish = System.currentTimeMillis();
      if (!addresses.isEmpty()) {
        this.apiClient.publish("/addresses", new ArrayList<>(addresses));
      }

      if (!assets.isEmpty()) {
        this.apiClient.publish("/assets", new ArrayList<>(assets));
      }

      if (!transactions.isEmpty()) {
        this.apiClient.publish("/transactions", transactions);
      }
      this.logPerformance("publish", timePublish);

      this.enrichBlock(block, transactions);
      this.logPerformance("ledger", timeLedger);
    }

    long timePublishLedgers = System.currentTimeMillis();
    this.apiClient.publishWithState("/blocks", blocks);
    this.logPerformance("publishLedgers", timePublishLedgers);

    this.logPerformance("ledgers", timeLedgers);

    if (this.historicalManager.getLastLedger() != null) {
      if (maxSequence > this.historicalManager.getLastLedger()) {
        throw new HorizonServer.StellarException(
            "Ending collection, current ledger > " + this.historicalManager.getLastLedger() + "."
        );
      }
    }
  }

  private void enrichBlock(BlockchainEntityWithState<Block> block, List<Transaction> transactions) {
    if (block.getEntity().getRewardFees() != null) {
      return;
    }

    BigDecimal fees = BigDecimal.ZERO;
    for (Transaction transaction : transactions) {
      fees = fees.add(transaction.getFees());
    }
    block.getEntity().setRewardFees(fees);
  }

  private Transaction enrichTransaction(
      TransactionResponse     transactionResponse,
      Map<Long, String>       effectLookup,
      List<OperationResponse> operationResponses
  ) {
    return this.modelMapper.mapTransaction(
      transactionResponse,
      effectLookup,
      operationResponses
    );
  }

  private List<OperationResponse> fetchOperationsForLedger(long ledger) {
    try {
      return StellarSubscriberConfiguration.getObjects(
        this.server,
        this.server.horizonServer()
          .operations()
          .forLedger(ledger)
          .limit(HorizonServer.HORIZON_PER_REQUEST_LIMIT)
          .execute(),
        "ledger.operations"
      );
    } catch (IOException | FormatException e) {
      LOG.error(
          "Unable to fetch information about operations for ledger " + ledger,
          e
      );
      Metrics.count("ledger.operations.errors", 1);
      return Collections.emptyList();
    }
  }

  private Collection<Address> collectAddresses(List<FunctionCall> functionCalls) {
    Map<String, Address> addresses = new HashMap<>();

    // TODO: report account balances properly to the inbound API

    for (FunctionCall functionCall : functionCalls) {
      final String from = functionCall.getFrom();
      final String to = functionCall.getTo();

      if (from != null && !from.isEmpty() && !addresses.containsKey(from)) {
        AccountResponse accountResponse = this.server.fetchAccountDetails(from);
        if (accountResponse != null) {
          addresses.put(
              from, this.modelMapper.mapAccount(accountResponse, functionCall.getTimestamp()));
        }
      }

      if (to != null && !to.isEmpty() && !addresses.containsKey(to)) {
        AccountResponse accountResponse = this.server.fetchAccountDetails(to);
        if (accountResponse != null) {
          addresses.put(
              to, this.modelMapper.mapAccount(accountResponse, functionCall.getTimestamp()));
        }
      }
    }

    return addresses.values();
  }

  private List<Asset> collectAssets(List<OperationResponse> operationResponses, Long ledger) {
    return this.modelMapper.mapAssets(operationResponses, ledger)
      .stream()
      .distinct()
      .map(this::enrichCachedAsset)
      .collect(Collectors.toList());
  }

  private Asset enrichCachedAsset(Asset asset) {
    String assetHash = asset.getType() + "_" + asset.getCode() + "_" + asset.getIssuerAccount();
    if (this.cache.containsKey(assetHash)) {
      return this.cache.get(assetHash);
    }

    asset = StellarSubscriberConfiguration.enrichAsset(this.server, asset);

    this.cache.put(assetHash, asset);

    return asset;
  }

  private String getCursorPointer() {
    if (this.historicalManager.disabled()) {
      return this.stateStorage.getStateToken(Block.class.getSimpleName(), () -> "now");
    } else {
      return this.historicalManager.ledgerPagingToken();
    }
  }

  private <T> void logPerformance(String metric, long startTime) {
    final long duration = System.currentTimeMillis() - startTime;
    LOG.debug("[PERFORMANCE] " + metric + " took " + duration + " ms");
    Metrics.mean("performance." + metric.toLowerCase(), duration);
  }

  private Map<Long, String> fetchEffectsForLedger(Long ledger) throws IOException {
    List<EffectResponse> effects = StellarSubscriberConfiguration.getObjects(
        this.server,
        this.server.horizonServer()
          .effects()
          .forLedger(ledger)
          .limit(HorizonServer.HORIZON_PER_REQUEST_LIMIT)
          .execute(),
        "ledger.operations.effects"
    );

    Map<Long, String> effectLookup = new HashMap<>();
    for (EffectResponse effect : effects) {
      final URL operationUri = new URL(effect.getLinks().getOperation().getHref());
      final String[] fields = operationUri.getPath().split("/");

      if (fields.length < 2 || !fields[fields.length - 2].equals("operations")) {
        LOG.error("unexpected operation href: {} fields: {} length: {} element: {}", operationUri, (Object)fields, fields.length, fields[fields.length - 2]);
        throw new RuntimeException("assertion failed for operation id extraction");
      }

      final Long operationId = Long.parseLong(fields[fields.length - 1]);
      String effectContents = effectLookup.getOrDefault(operationId, "");

      if (!effectContents.isEmpty()) {
        effectContents += ",";
      }
      effectContents += effect.getType();
      effectLookup.put(operationId, effectContents);
    }

    return effectLookup;
  }
}
