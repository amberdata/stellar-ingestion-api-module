package io.amberdata.inbound.stellar.mapper.operations;

import io.amberdata.inbound.domain.Asset;
import io.amberdata.inbound.domain.FunctionCall;
import io.amberdata.inbound.stellar.client.HorizonServer;
import io.amberdata.inbound.stellar.mapper.AssetMapper;

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
import org.stellar.sdk.responses.operations.CreatePassiveOfferOperationResponse;
import org.stellar.sdk.responses.operations.InflationOperationResponse;
import org.stellar.sdk.responses.operations.ManageDataOperationResponse;
import org.stellar.sdk.responses.operations.ManageOfferOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PathPaymentOperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;
import org.stellar.sdk.responses.operations.SetOptionsOperationResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OperationMapperManager {

  private static final Logger LOG = LoggerFactory.getLogger(OperationMapperManager.class);

  private final Map<Class<? extends OperationResponse>, OperationMapper> responsesMap;
  private final HorizonServer server;

  @Autowired
  public OperationMapperManager(AssetMapper assetMapper, HorizonServer server) {
    responsesMap = new HashMap<>();
    responsesMap.put(CreateAccountOperationResponse.class, new CreateAccountOperationMapper());
    responsesMap.put(PaymentOperationResponse.class, new PaymentOperationMapper(assetMapper));
    responsesMap.put(
        PathPaymentOperationResponse.class,
        new PathPaymentOperationMapper(assetMapper)
    );
    responsesMap.put(
        ManageOfferOperationResponse.class,
        new ManageOfferOperationMapper(assetMapper)
    );
    responsesMap.put(
        CreatePassiveOfferOperationResponse.class,
        new CreatePassiveOfferOperationMapper(assetMapper)
    );
    responsesMap.put(SetOptionsOperationResponse.class, new SetOptionsOperationMapper());
    responsesMap.put(
        ChangeTrustOperationResponse.class,
        new ChangeTrustOperationMapper(assetMapper)
    );
    responsesMap.put(AllowTrustOperationResponse.class, new AllowTrustOperationMapper(assetMapper));
    responsesMap.put(AccountMergeOperationResponse.class, new AccountMergeOperationMapper());
    responsesMap.put(InflationOperationResponse.class, new InflationOperationMapper());
    responsesMap.put(ManageDataOperationResponse.class, new ManageDataOperationMapper());
    responsesMap.put(BumpSequenceOperationResponse.class, new BumpSequenceOperationMapper());

    this.server = server;
  }

  public String generateOperationHash(
      long ledgerNumber,
      String transactionHash,
      int transactionIndex
  ) {
    return
      String.valueOf(ledgerNumber) + "_"
      + transactionHash + "_"
      + String.valueOf(transactionIndex);
  }

  public FunctionCall map(OperationResponse operationResponse, Long ledger, Integer index) {
    OperationMapper operationMapper = responsesMap.get(operationResponse.getClass());

    List<String> effects = fetchEffectsForOperation(operationResponse);

    String transactionHash = operationResponse.getTransactionHash();

    FunctionCall functionCall;
    if (operationMapper == null) {
      LOG.warn(
          "An unknown operation for ledger " + ledger + " and transaction "
          + transactionHash + " has been introduced which is not implemented: "
          + operationResponse.getClass().getSimpleName()
      );
      functionCall = new FunctionCall();
      functionCall.setName("unknown");
      functionCall.setSignature("unknown_operation");
      functionCall.setArguments(Collections.emptyList());
    } else {
      functionCall = operationMapper.map(operationResponse);
    }

    functionCall.setBlockNumber(ledger);
    functionCall.setTransactionHash(transactionHash);
    functionCall.setTimestamp(Instant.parse(operationResponse.getCreatedAt()).toEpochMilli());
    functionCall.setDepth(0);
    functionCall.setIndex(index);
    functionCall.setHash(this.generateOperationHash(ledger, transactionHash, index));
    functionCall.setResult(String.join(",", effects));

    return functionCall;
  }

  public List<Asset> mapAssets(long ledger, OperationResponse operationResponse) {
    OperationMapper operationMapper = responsesMap.get(operationResponse.getClass());
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

  private List<String> fetchEffectsForOperation(OperationResponse operationResponse) {
    try {
      return server.horizonServer()
          .effects()
          .forOperation(operationResponse.getId())
          .execute()
          .getRecords()
          .stream()
          .map(EffectResponse::getType)
          .collect(Collectors.toList());
    } catch (IOException ex) {
      return Collections.emptyList();
    }
  }
}
