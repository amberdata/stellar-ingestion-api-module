package io.amberdata.inbound.stellar.mapper.operations;

import io.amberdata.inbound.domain.Asset;
import io.amberdata.inbound.domain.FunctionCall;
import io.amberdata.inbound.stellar.mapper.AssetMapper;

import java.math.BigDecimal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.stellar.sdk.PathPaymentStrictReceiveOperation;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PathPaymentStrictReceiveOperationResponse;

public class PathPaymentOperationMapper implements OperationMapper {

  private static final Logger LOG =
      LoggerFactory.getLogger(PathPaymentOperationMapper.class);

  private AssetMapper assetMapper;

  public PathPaymentOperationMapper(AssetMapper assetMapper) {
    this.assetMapper = assetMapper;
  }

  @Override
  @SuppressWarnings("checkstyle:MethodParamPad")
  public FunctionCall map(OperationResponse operationResponse) {
    PathPaymentStrictReceiveOperationResponse response =
        (PathPaymentStrictReceiveOperationResponse) operationResponse;

    if (response.getAsset() == null) {
      LOG.warn("Asset in PathPaymentStrictReceiveOperationResponse is null");
    }

    if (response.getFrom() == null) {
      LOG.warn("Source account in PathPaymentStrictReceiveOperationResponse is null");
    }

    if (response.getTo() == null) {
      LOG.warn("Destination account in PathPaymentStrictReceiveOperationResponse is null");
    }

    Asset asset       = this.assetMapper.map(response.getAsset());
    Asset sourceAsset = this.assetMapper.map(response.getSourceAsset());

    String to = this.fetchAccountId(response.getTo());

    // If both source and destination are Lumens, we only want to count this transfer once,
    // hence the "if/else if" construct instead of "if/if".
    BigDecimal lumensTransferred;
    if ("native".equals(response.getSourceAsset().getType())) {
      lumensTransferred = new BigDecimal(response.getSourceAmount());
    } else if ("native".equals(response.getAsset().getType())) {
      lumensTransferred = new BigDecimal(response.getAmount());
    } else {
      lumensTransferred = BigDecimal.ZERO;
    }

    return new FunctionCall.Builder()
        .from             (this.fetchAccountId(response.getFrom()))
        .to               (to)
        .type             (PathPaymentStrictReceiveOperation.class.getSimpleName())
        .assetType        (asset.getCode())
        .value            (response.getAmount())
        .lumensTransferred(lumensTransferred)
        .meta             (this.getOptionalProperties(response, asset))
        .signature        (
            "path_payment(asset, integer, account_id, asset, integer, list_of_assets)"
        )
        .arguments        (
            Arrays.asList(
                FunctionCall.Argument.from("send_asset",         sourceAsset.getCode()),
                FunctionCall.Argument.from("send_max",           response.getSourceMax()),
                FunctionCall.Argument.from("destination",        to),
                FunctionCall.Argument.from("destination_asset",  asset.getCode()),
                FunctionCall.Argument.from("destination_amount", response.getAmount())
            // FunctionCall.Argument.from("path", "") - no path in response
            )
        )
        .build();
  }

  private String fetchAccountId(String from) {
    return from != null ? from : "";
  }

  @Override
  public List<Asset> getAssets(OperationResponse operationResponse) {
    PathPaymentStrictReceiveOperationResponse response =
        (PathPaymentStrictReceiveOperationResponse) operationResponse;

    Asset asset       = this.assetMapper.map(response.getAsset());
    Asset sourceAsset = this.assetMapper.map(response.getSourceAsset());

    return Arrays.asList(asset, sourceAsset);
  }

  private Map<String, Object> getOptionalProperties(
      PathPaymentStrictReceiveOperationResponse response,
      Asset                                     asset
  ) {
    Asset sourceAsset = this.assetMapper.map(response.getSourceAsset());

    Map<String, Object> optionalProperties = new HashMap<>();
    optionalProperties.put("stellarAssetType",       asset.getType().getName());
    optionalProperties.put("assetIssuer",            asset.getIssuerAccount());
    optionalProperties.put("sourceAsset",            sourceAsset.getCode());
    optionalProperties.put("stellarSourceAssetType", sourceAsset.getType().getName());
    optionalProperties.put("sourceAssetIssuer",      sourceAsset.getIssuerAccount());
    optionalProperties.put("sourceMax",              response.getSourceMax());
    return optionalProperties;
  }

}
