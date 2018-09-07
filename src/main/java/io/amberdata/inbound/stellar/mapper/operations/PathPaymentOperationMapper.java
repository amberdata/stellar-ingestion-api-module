package io.amberdata.inbound.stellar.mapper.operations;

import io.amberdata.inbound.domain.Asset;
import io.amberdata.inbound.domain.FunctionCall;
import io.amberdata.inbound.stellar.mapper.AssetMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.stellar.sdk.KeyPair;
import org.stellar.sdk.PathPaymentOperation;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PathPaymentOperationResponse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PathPaymentOperationMapper implements OperationMapper {

  private static final Logger LOG = LoggerFactory.getLogger(PathPaymentOperationMapper.class);

  private AssetMapper assetMapper;

  public PathPaymentOperationMapper(AssetMapper assetMapper) {
    this.assetMapper = assetMapper;
  }

  @Override
  public FunctionCall map(OperationResponse operationResponse) {
    PathPaymentOperationResponse response = (PathPaymentOperationResponse) operationResponse;

    if (response.getAsset() == null) {
      LOG.warn("Asset in PathPaymentOperationResponse is null");
    }

    if (response.getFrom() == null) {
      LOG.warn("Source account in PathPaymentOperationResponse is null");
    }

    if (response.getTo() == null) {
      LOG.warn("Destination account in PathPaymentOperationResponse is null");
    }

    Asset asset = assetMapper.map(response.getAsset());
    Asset sourceAsset = assetMapper.map(response.getSourceAsset());

    return new FunctionCall.Builder()
        .from(fetchAccountId(response.getFrom()))
        .to(fetchAccountId(response.getTo()))
        .type(PathPaymentOperation.class.getSimpleName())
        .assetType(asset.getCode())
        .value(response.getAmount())
        .meta(getOptionalProperties(response, asset))
        .signature("path_payment(asset, integer, account_id, asset, integer, list_of_assets)")
        .arguments(
            Arrays.asList(
                FunctionCall.Argument.from("send_asset", sourceAsset.getCode()),
                FunctionCall.Argument.from("send_max", response.getSourceMax()),
                FunctionCall.Argument.from("destination", fetchAccountId(response.getTo())),
                FunctionCall.Argument.from("destination_asset", asset.getCode()),
                FunctionCall.Argument.from("destination_amount", response.getAmount())
                // FunctionCall.Argument.from("path", "") - no path in response
            )
        )
        .build();
  }

  private String fetchAccountId(KeyPair from) {
    return from != null ? from.getAccountId() : "";
  }

  @Override
  public List<Asset> getAssets(OperationResponse operationResponse) {
    PathPaymentOperationResponse response = (PathPaymentOperationResponse) operationResponse;

    Asset asset = assetMapper.map(response.getAsset());
    Asset sourceAsset = assetMapper.map(response.getSourceAsset());
    return Arrays.asList(asset, sourceAsset);
  }

  private Map<String, Object> getOptionalProperties(
      PathPaymentOperationResponse response,
      Asset asset
  ) {
    Asset sourceAsset = assetMapper.map(response.getSourceAsset());

    Map<String, Object> optionalProperties = new HashMap<>();
    optionalProperties.put("stellarAssetType", asset.getType().getName());
    optionalProperties.put("assetIssuer", asset.getIssuerAccount());

    optionalProperties.put("sourceAsset", sourceAsset.getCode());
    optionalProperties.put("stellarSourceAssetType", sourceAsset.getType().getName());
    optionalProperties.put("sourceAssetIssuer", sourceAsset.getIssuerAccount());
    optionalProperties.put("sourceMax", response.getSourceMax());

    return optionalProperties;
  }
}
