package io.amberdata.inbound.stellar.mapper.operations;

import io.amberdata.inbound.domain.Asset;
import io.amberdata.inbound.domain.FunctionCall;
import io.amberdata.inbound.stellar.client.HorizonServer;
import io.amberdata.inbound.stellar.mapper.AssetMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.stellar.sdk.responses.effects.EffectResponse;
import org.stellar.sdk.responses.operations.AccountMergeOperationResponse;
import org.stellar.sdk.responses.operations.AllowTrustOperationResponse;
import org.stellar.sdk.responses.operations.BumpSequenceOperationResponse;
import org.stellar.sdk.responses.operations.ChangeTrustOperationResponse;
import org.stellar.sdk.responses.operations.CreateAccountOperationResponse;
import org.stellar.sdk.responses.operations.CreatePassiveSellOfferOperationResponse;
import org.stellar.sdk.responses.operations.InflationOperationResponse;
import org.stellar.sdk.responses.operations.ManageBuyOfferOperationResponse;
import org.stellar.sdk.responses.operations.ManageDataOperationResponse;
import org.stellar.sdk.responses.operations.ManageSellOfferOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PathPaymentStrictReceiveOperationResponse;
import org.stellar.sdk.responses.operations.PathPaymentStrictSendOperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;
import org.stellar.sdk.responses.operations.SetOptionsOperationResponse;

@Component
public class OperationMapperManager {

  private static final Logger LOG = LoggerFactory.getLogger(OperationMapperManager.class);

  private final Map<Class<? extends OperationResponse>, OperationMapper> responsesMap;
  private final HorizonServer                                            server;

  /**
   * Default constructor.
   *
   * @param assetMapper the asset mapper
   * @param server      the Horizon server
   */
  @Autowired
  public OperationMapperManager(AssetMapper assetMapper, HorizonServer server) {
    this.responsesMap = new HashMap<>();
    this.add(AccountMergeOperationResponse.class,  new AccountMergeOperationMapper(server));
    this.add(AllowTrustOperationResponse.class,    new AllowTrustOperationMapper(assetMapper));
    this.add(BumpSequenceOperationResponse.class,  new BumpSequenceOperationMapper());
    this.add(ChangeTrustOperationResponse.class,   new ChangeTrustOperationMapper(assetMapper));
    this.add(CreateAccountOperationResponse.class, new CreateAccountOperationMapper());
    this.add(
        CreatePassiveSellOfferOperationResponse.class,
        new CreatePassiveOfferOperationMapper(assetMapper)
    );
    this.add(InflationOperationResponse.class,      new InflationOperationMapper());
    this.add(ManageBuyOfferOperationResponse.class, new ManageBuyOfferOperationMapper(assetMapper));
    this.add(ManageDataOperationResponse.class,     new ManageDataOperationMapper());
    this.add(
        ManageSellOfferOperationResponse.class,
        new ManageSellOfferOperationMapper(assetMapper)
    );
    this.add(
        PathPaymentStrictReceiveOperationResponse.class,
        new PathPaymentOperationMapper(assetMapper)
    );
    this.add(
        PathPaymentStrictReceiveOperationResponse.class,
        new PathPaymentStrictReceiveOperationMapper(assetMapper)
    );
    this.add(
        PathPaymentStrictSendOperationResponse.class,
        new PathPaymentStrictSendOperationMapper(assetMapper)
    );
    this.add(PaymentOperationResponse.class,    new PaymentOperationMapper(assetMapper));
    this.add(SetOptionsOperationResponse.class, new SetOptionsOperationMapper());

    this.server = server;
  }

  /**
   * Generates the hash of the operation.
   *
   * @param ledgerNumber     the ledger number
   * @param transactionHash  the hash of the transaction
   * @param transactionIndex the index of the transaction
   *
   * @return the hash of the operation.
   */
  public String generateOperationHash(
      long   ledgerNumber,
      String transactionHash,
      int    transactionIndex
  ) {
    return
      String.valueOf(ledgerNumber) + "_"
      + transactionHash + "_"
      + String.valueOf(transactionIndex);
  }

  /**
   * Extracts the function call from the specified operation.
   *
   * @param operationResponse the operation response
   * @param ledger            the ledger number
   * @param index             the index of the function call
   *
   * @return the function call.
   */
  @SuppressWarnings("checkstyle:MethodParamPad")
  public FunctionCall map(OperationResponse operationResponse, Long ledger, Integer index) {
    OperationMapper operationMapper = this.responsesMap.get(operationResponse.getClass());

    String transactionHash = operationResponse.getTransactionHash();

    FunctionCall functionCall;
    if (operationMapper == null) {
      LOG.warn(
          "An unknown operation for ledger " + ledger + " and transaction "
          + transactionHash + " has been introduced which is not implemented: "
          + operationResponse.getClass().getSimpleName()
      );
      functionCall = new FunctionCall();
      functionCall.setName     ("unknown");
      functionCall.setSignature("unknown_operation");
      functionCall.setArguments(Collections.emptyList());
    } else {
      functionCall = operationMapper.map(operationResponse);
    }

    List<String> effects = this.fetchEffectsForOperation(operationResponse);

    functionCall.setBlockNumber    (ledger);
    functionCall.setTransactionHash(transactionHash);
    functionCall.setTimestamp      (Instant.parse(operationResponse.getCreatedAt()).toEpochMilli());
    functionCall.setDepth          (0);
    functionCall.setIndex          (index);
    functionCall.setHash           (this.generateOperationHash(ledger, transactionHash, index));
    functionCall.setResult         (String.join(",", effects));

    return functionCall;
  }

  /**
   * Gets the assets associated to the specified operation.
   *
   * @param ledger            the ledger number
   * @param operationResponse the operation response
   *
   * @return the list of assets for the specified operation.
   */
  public List<Asset> mapAssets(long ledger, OperationResponse operationResponse) {
    OperationMapper operationMapper = this.responsesMap.get(operationResponse.getClass());
    if (operationMapper == null) {
      LOG.warn(
          "An unknown operation for ledger " + ledger + " and transaction "
          + operationResponse.getTransactionHash()
          + " has been introduced which is not implemented: "
          + operationResponse.getClass().getSimpleName()
      );
      return Collections.emptyList();
    }

    return operationMapper.getAssets(operationResponse);
  }

  private void add(Class<? extends OperationResponse> type, OperationMapper mapper) {
    this.responsesMap.put(type, mapper);
  }

  @SuppressWarnings("checkstyle:MethodParamPad")
  private List<String> fetchEffectsForOperation(OperationResponse operationResponse) {
    try {
      return this.server.horizonServer()
        .effects     ()
        .forOperation(operationResponse.getId())
        .execute     ()
        .getRecords  ()
        .stream      ()
        .map         (EffectResponse::getType)
        .collect     (Collectors.toList());
    } catch (IOException ioe) {
      return Collections.emptyList();
    }
  }

}
