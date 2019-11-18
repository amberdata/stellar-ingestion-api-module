package io.amberdata.inbound.stellar.mapper.operations;

import io.amberdata.inbound.domain.Asset;
import io.amberdata.inbound.domain.FunctionCall;
import io.amberdata.inbound.stellar.mapper.AssetMapper;

import java.math.BigDecimal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.stellar.sdk.PaymentOperation;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;

public class PaymentOperationMapper implements OperationMapper {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentOperationMapper.class);

  private AssetMapper assetMapper;

  public PaymentOperationMapper(AssetMapper assetMapper) {
    this.assetMapper = assetMapper;
  }

  @Override
  @SuppressWarnings("checkstyle:MethodParamPad")
  public FunctionCall map(OperationResponse operationResponse) {
    PaymentOperationResponse response = (PaymentOperationResponse) operationResponse;

    if (response.getAsset() == null) {
      LOG.warn("Asset in PaymentOperationResponse is null");
    }

    if (response.getFrom() == null) {
      LOG.warn("Source account in PaymentOperationResponse is null");
    }

    if (response.getTo() == null) {
      LOG.warn("Destination account in PaymentOperationResponse is null");
    }

    Asset asset = this.assetMapper.map(response.getAsset());

    BigDecimal lumensTransferred = "native".equals(response.getAsset().getType())
        ? new BigDecimal(response.getAmount())
        : BigDecimal.ZERO;

    return new FunctionCall.Builder()
        .from             (this.fetchAccountId(response.getFrom()))
        .to               (this.fetchAccountId(response.getTo()))
        .type             (PaymentOperation.class.getSimpleName())
        .assetType        (asset.getCode())
        .value            (response.getAmount())
        .lumensTransferred(lumensTransferred)
        .meta             (this.getOptionalProperties(asset))
        .signature        ("payment(account_id, asset, integer)")
        .arguments        (
            Arrays.asList(
                FunctionCall.Argument.from("destination", this.fetchAccountId(response.getTo())),
                FunctionCall.Argument.from("asset",       asset.getCode()),
                FunctionCall.Argument.from("amount",      response.getAmount())
            )
        )
        .build();
  }

  private String fetchAccountId(String from) {
    return from != null ? from : "";
  }

  @Override
  public List<Asset> getAssets(OperationResponse operationResponse) {
    PaymentOperationResponse response = (PaymentOperationResponse) operationResponse;

    Asset asset = this.assetMapper.map(response.getAsset());

    return Collections.singletonList(asset);
  }

  private Map<String, Object> getOptionalProperties(Asset asset) {
    Map<String, Object> optionalProperties = new HashMap<>();
    optionalProperties.put("stellarAssetType", asset.getType().getName());
    optionalProperties.put("assetIssuer",      asset.getIssuerAccount());
    return optionalProperties;
  }

}
