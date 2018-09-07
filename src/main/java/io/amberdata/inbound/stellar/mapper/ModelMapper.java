package io.amberdata.inbound.stellar.mapper;

import io.amberdata.inbound.core.client.BlockchainEntityWithState;
import io.amberdata.inbound.core.state.entities.ResourceState;
import io.amberdata.inbound.domain.Address;
import io.amberdata.inbound.domain.Asset;
import io.amberdata.inbound.domain.Block;
import io.amberdata.inbound.domain.FunctionCall;
import io.amberdata.inbound.domain.Order;
import io.amberdata.inbound.domain.Trade;
import io.amberdata.inbound.domain.Transaction;
import io.amberdata.inbound.stellar.configuration.subscribers.ExtendedTradeResponse;
import io.amberdata.inbound.stellar.mapper.operations.OperationMapperManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.LedgerResponse;
import org.stellar.sdk.responses.TradeResponse;
import org.stellar.sdk.responses.TransactionResponse;
import org.stellar.sdk.responses.operations.CreatePassiveOfferOperationResponse;
import org.stellar.sdk.responses.operations.ManageOfferOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.time.Instant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class ModelMapper {

  private static final Logger LOG = LoggerFactory.getLogger(ModelMapper.class);

  private final OperationMapperManager operationMapperManager;
  private final AssetMapper assetMapper;

  @Autowired
  public ModelMapper(OperationMapperManager operationMapperManager, AssetMapper assetMapper) {
    this.operationMapperManager = operationMapperManager;
    this.assetMapper = assetMapper;
  }

  public Block mapLedger(LedgerResponse ledgerResponse) {
    return new Block.Builder()
        .number(BigInteger.valueOf(ledgerResponse.getSequence()))
        .hash(ledgerResponse.getHash())
        .parentHash(ledgerResponse.getPrevHash())
        .gasUsed(new BigDecimal(ledgerResponse.getFeePool()))
        .numTransactions(ledgerResponse.getTransactionCount())
        .timestamp(Instant.parse(ledgerResponse.getClosedAt()).toEpochMilli())
        .meta(blockMetaProperties(ledgerResponse))
        .build();
  }

  public BlockchainEntityWithState<Block> mapLedgerWithState(LedgerResponse ledgerResponse) {
    return BlockchainEntityWithState.from(
        this.mapLedger(ledgerResponse),
        ResourceState.from(Block.class.getSimpleName(), ledgerResponse.getPagingToken())
    );
  }

  public Transaction mapTransaction(
      TransactionResponse transactionResponse,
      List<OperationResponse> operationResponses
  ) {
    List<FunctionCall> functionCalls = this.mapOperations(
        operationResponses,
        transactionResponse.getLedger()
    );
    Set<String> tos = functionCalls.stream().map(FunctionCall::getTo).collect(Collectors.toSet());

    String to = "";
    if (tos.size() == 1) {
      to = tos.iterator().next();
    }
    if (tos.size() > 1) {
      to = "_";
    }

    return new Transaction.Builder()
        .hash(transactionResponse.getHash())
        .transactionIndex(transactionResponse.getSourceAccountSequence())
        .nonce(BigInteger.valueOf(transactionResponse.getSourceAccountSequence()))
        .blockNumber(BigInteger.valueOf(transactionResponse.getLedger()))
        .from(transactionResponse.getSourceAccount().getAccountId())
        .to(to)
        .tos(new ArrayList<>(tos))
        .gasUsed(BigInteger.valueOf(transactionResponse.getFeePaid()))
        .numLogs(transactionResponse.getOperationCount())
        .timestamp(Instant.parse(transactionResponse.getCreatedAt()).toEpochMilli())
        .functionCalls(functionCalls)
        .status("0x1")
        .value(BigDecimal.ZERO)
        .build();
  }

  public BlockchainEntityWithState<Transaction> mapTransactionWithState(
      TransactionResponse transactionResponse,
      List<OperationResponse> operationResponses
  ) {
    return BlockchainEntityWithState.from(
        this.mapTransaction(transactionResponse, operationResponses),
        ResourceState.from(Transaction.class.getSimpleName(), transactionResponse.getPagingToken())
    );
  }

  public List<FunctionCall> mapOperations(List<OperationResponse> operationResponses, Long ledger) {
    return IntStream
        .range(0, operationResponses.size())
        .mapToObj(
            index -> this.operationMapperManager.map(operationResponses.get(index), ledger, index)
        )
        .collect(Collectors.toList());
  }

  public List<Asset> mapAssets(List<OperationResponse> operationResponses, Long ledger) {
    List<Asset> allAssets = new ArrayList<>();
    for (int i = 0; i < operationResponses.size(); i++) {
      OperationResponse operationResponse = operationResponses.get(i);
      String transactionHash = operationResponse.getTransactionHash();
      List<Asset> assets = this.operationMapperManager.mapAssets(ledger, operationResponse);
      for (Asset asset : assets) {
        asset.setTimestamp(Instant.parse(operationResponse.getCreatedAt()).toEpochMilli());
        asset.setTransactionHash(transactionHash);
        asset.setFunctionCallHash(
            this.operationMapperManager.generateOperationHash(ledger, transactionHash, i)
        );
      }
      allAssets.addAll(assets);
    }
    return allAssets;
  }

  public Address mapAccount(AccountResponse accountResponse, Long timestamp) {
    return new Address.Builder()
        .hash(accountResponse.getKeypair().getAccountId())
        .timestamp(timestamp)
        .meta(addressMetaProperties(accountResponse))
        .build();
  }

  public BlockchainEntityWithState<Address> mapAccountWithState(
      AccountResponse accountResponse,
      Long timestamp,
      String pagingToken
  ) {
    return BlockchainEntityWithState.from(
        this.mapAccount(accountResponse, timestamp),
        ResourceState.from(Address.class.getSimpleName(), pagingToken)
    );
  }

  public List<Order> mapOrders(List<OperationResponse> operationResponses, Long ledger) {
    List<Order> orders = new ArrayList<>();
    for (int i = 0; i < operationResponses.size(); i++) {
      OperationResponse operationResponse = operationResponses.get(i);
      if (
          operationResponse.getClass() == ManageOfferOperationResponse.class
              || operationResponse.getClass() == CreatePassiveOfferOperationResponse.class
          ) {
        ManageOfferOperationResponse response = (ManageOfferOperationResponse) operationResponse;

        if (response.getOfferId() == 0) {
          continue;
        }

        Asset sellingAsset = assetMapper.map(response.getSellingAsset());
        Asset buyingAsset = assetMapper.map(response.getBuyingAsset());

        Order order = new Order.Builder()
            .type(0)
            .orderId(response.getOfferId().toString())
            .blockNumber(ledger)
            .transactionHash(response.getTransactionHash())
            .functionCallHash(
                String.valueOf(ledger) + "_"
                    + operationResponse.getTransactionHash() + "_"
                    + String.valueOf(i)
            )
            .makerAddress(
                response.getSourceAccount() != null
                    ? response.getSourceAccount().getAccountId()
                    : ""
            )
            .sellAsset(
                sellingAsset.getType() == Asset.AssetType.ASSET_TYPE_NATIVE
                    ? "native"
                    : sellingAsset.getIssuerAccount() + "." + sellingAsset.getCode()
            )
            .buyAsset(
                buyingAsset.getType() == Asset.AssetType.ASSET_TYPE_NATIVE
                    ? "native"
                    : buyingAsset.getIssuerAccount() + "." + buyingAsset.getCode()
            )
            .buyAmount(BigDecimal.ZERO)
            .sellAmount(new BigDecimal(response.getAmount()))
            .timestamp(Instant.parse(response.getCreatedAt()).toEpochMilli())
            .meta(Collections.singletonMap("buying_price", response.getPrice()))
            .build();

        orders.add(order);
      }
    }
    return orders;
  }

  public List<Trade> mapTrades(List<ExtendedTradeResponse> records) {
    return records.stream()
        .map(this::mapTrade)
        .collect(Collectors.toList());
  }

  private Map<String, Object> blockMetaProperties(LedgerResponse ledgerResponse) {
    Map<String, Object> metaProperties = new HashMap<>();

    metaProperties.put("operation_count", ledgerResponse.getOperationCount());
    metaProperties.put("total_coins", ledgerResponse.getTotalCoins());
    metaProperties.put("base_fee_in_stroops", ledgerResponse.getBaseFeeInStroops());
    metaProperties.put("base_reserve_in_stroops", ledgerResponse.getBaseReserveInStroops());
    metaProperties.put("max_tx_set_size", ledgerResponse.getMaxTxSetSize());
    metaProperties.put("sequence", ledgerResponse.getSequence());

    return metaProperties;
  }

  private Map<String, Object> addressMetaProperties(AccountResponse accountResponse) {
    Map<String, Object> metaProperties = new HashMap<>();

    metaProperties.put("sequence", accountResponse.getSequenceNumber());
    metaProperties.put("subentry_count", accountResponse.getSubentryCount());
    metaProperties.put("threshold_low", accountResponse.getThresholds().getLowThreshold());
    metaProperties.put("threshold_med", accountResponse.getThresholds().getMedThreshold());
    metaProperties.put("threshold_high", accountResponse.getThresholds().getHighThreshold());
    metaProperties.put("flag_auth_required", accountResponse.getFlags().getAuthRequired());
    metaProperties.put("flag_auth_revocable", accountResponse.getFlags().getAuthRevocable());
    metaProperties.put(
        "balances",
        Arrays
          .stream(accountResponse.getBalances())
          .map(this::balanceProperty)
          .collect(Collectors.toList())
    );
    metaProperties.put(
        "signers",
        Arrays
          .stream(accountResponse.getSigners())
          .map(this::signerProperty)
          .collect(Collectors.toList())
    );

    return metaProperties;
  }

  private Map<String, Object> balanceProperty(AccountResponse.Balance balance) {
    Map<String, Object> optionalProperties = new HashMap<>();

    optionalProperties.put("balance", balance.getBalance());
    optionalProperties.put("limit", balance.getLimit());
    optionalProperties.put("asset_type", balance.getAssetType());
    if (!balance.getAssetType().equals("native")) {
      optionalProperties.put("asset_code", balance.getAssetCode());
      if (balance.getAssetIssuer() == null) {
        LOG.warn("AssetIssuer in mapping balance property is null");
      } else {
        optionalProperties.put("asset_issuer", balance.getAssetIssuer().getAccountId());
      }
    }

    return optionalProperties;
  }

  private Map<String, Object> signerProperty(AccountResponse.Signer signer) {
    Map<String, Object> optionalProperties = new HashMap<>();

    optionalProperties.put("public_key", signer.getKey());
    optionalProperties.put("weight", String.valueOf(signer.getWeight()));

    return optionalProperties;
  }

  private Trade mapTrade(ExtendedTradeResponse extendedTradeResponse) {
    TradeResponse tradeResponse = extendedTradeResponse.getTradeResponse();

    String baseAccount = tradeResponse.getBaseAccount() != null
        ? tradeResponse.getBaseAccount().getAccountId()
        : "";
    String counterAccount = tradeResponse.getCounterAccount() != null
        ? tradeResponse.getCounterAccount().getAccountId()
        : "";

    Asset buyAsset = assetMapper.map(
        tradeResponse.isBaseSeller()
          ? tradeResponse.getCounterAsset()
          : tradeResponse.getBaseAsset()
    );
    Asset sellAsset = assetMapper.map(
        tradeResponse.isBaseSeller()
          ? tradeResponse.getBaseAsset()
          : tradeResponse.getCounterAsset()
    );

    BigDecimal buyAmount = new BigDecimal(
        tradeResponse.isBaseSeller()
          ? tradeResponse.getCounterAmount()
          : tradeResponse.getBaseAmount()
    );
    BigDecimal sellAmount = new BigDecimal(
        tradeResponse.isBaseSeller()
          ? tradeResponse.getBaseAmount()
          : tradeResponse.getCounterAmount()
    );

    return new Trade.Builder()
      .tradeId(tradeResponse.getId())
      .type(tradeResponse.isBaseSeller() ? 1 : 0)
      .buyAddress(tradeResponse.isBaseSeller() ? counterAccount : baseAccount)
      .sellAddress(tradeResponse.isBaseSeller() ? baseAccount : counterAccount)
      .buyAsset(
        buyAsset.getType() == Asset.AssetType.ASSET_TYPE_NATIVE
          ? "native"
          : buyAsset.getIssuerAccount() + "." + buyAsset.getCode()
      )
      .sellAsset(
        sellAsset.getType() == Asset.AssetType.ASSET_TYPE_NATIVE
          ? "native"
          : sellAsset.getIssuerAccount() + "." + sellAsset.getCode()
      )
      .buyAmount(buyAmount)
      .sellAmount(sellAmount)
      .timestamp(Instant.parse(tradeResponse.getLedgerCloseTime()).toEpochMilli())
      .orderId(tradeResponse.getOfferId())
      .blockNumber(extendedTradeResponse.getLedger())
      .transactionHash(extendedTradeResponse.getTransactionHash())
      .functionCallHash(extendedTradeResponse.getOperationHash())
      .meta(Collections.singletonMap("price", tradeResponse.getPrice()))
      .build();
  }
}
