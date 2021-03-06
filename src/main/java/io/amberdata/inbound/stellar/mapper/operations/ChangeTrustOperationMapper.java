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

import org.stellar.sdk.ChangeTrustOperation;
import org.stellar.sdk.responses.operations.ChangeTrustOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

public class ChangeTrustOperationMapper implements OperationMapper {

  private static final Logger LOG = LoggerFactory.getLogger(ChangeTrustOperationMapper.class);

  private AssetMapper assetMapper;

  public ChangeTrustOperationMapper(AssetMapper assetMapper) {
    this.assetMapper = assetMapper;
  }

  @Override
  @SuppressWarnings("checkstyle:MethodParamPad")
  public FunctionCall map(OperationResponse operationResponse) {
    ChangeTrustOperationResponse response = (ChangeTrustOperationResponse) operationResponse;

    if (response.getAsset() == null) {
      LOG.warn("Asset in ChangeTrustOperationResponse is null");
    }

    if (response.getTrustor() == null) {
      LOG.warn("Trustor account in ChangeTrustOperationResponse is null");
    }

    if (response.getTrustee() == null) {
      LOG.warn("Trustee account in ChangeTrustOperationResponse is null");
    }

    Asset asset = this.assetMapper.map(response.getAsset());

    return new FunctionCall.Builder()
        .from             (this.fetchAccountId(response.getTrustor()))
        .to               (this.fetchAccountId(response.getTrustee()))
        .type             (ChangeTrustOperation.class.getSimpleName())
        .assetType        (asset.getCode())
        .lumensTransferred(BigDecimal.ZERO)
        .meta             (this.getOptionalProperties(response, asset))
        .signature        ("change_trust(asset, integer)")
        .arguments        (
            Arrays.asList(
                FunctionCall.Argument.from("line",  asset.getCode()),
                FunctionCall.Argument.from("limit", response.getLimit())
            )
        )
        .build();
  }

  private String fetchAccountId(String trustor) {
    return trustor != null ? trustor : "";
  }

  @Override
  public List<Asset> getAssets(OperationResponse operationResponse) {
    ChangeTrustOperationResponse response = (ChangeTrustOperationResponse) operationResponse;

    Asset asset = this.assetMapper.map(response.getAsset());
    return Collections.singletonList(asset);
  }

  private Map<String, Object> getOptionalProperties(
      ChangeTrustOperationResponse response,
      Asset                        asset
  ) {
    Map<String, Object> optionalProperties = new HashMap<>();
    optionalProperties.put("limit",            response.getLimit());
    optionalProperties.put("stellarAssetType", asset.getType().getName());
    optionalProperties.put("assetIssuer",      asset.getIssuerAccount());
    return optionalProperties;
  }

}
