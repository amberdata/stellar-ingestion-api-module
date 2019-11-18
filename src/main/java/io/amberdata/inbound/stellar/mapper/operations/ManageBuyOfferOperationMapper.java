package io.amberdata.inbound.stellar.mapper.operations;

import io.amberdata.inbound.domain.Asset;
import io.amberdata.inbound.domain.FunctionCall;
import io.amberdata.inbound.stellar.mapper.AssetMapper;

import java.math.BigDecimal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.stellar.sdk.ManageBuyOfferOperation;
import org.stellar.sdk.responses.operations.ManageBuyOfferOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

public class ManageBuyOfferOperationMapper implements OperationMapper {

  private static final Logger LOG = LoggerFactory.getLogger(ManageBuyOfferOperationMapper.class);

  private AssetMapper assetMapper;

  public ManageBuyOfferOperationMapper(AssetMapper assetMapper) {
    this.assetMapper = assetMapper;
  }

  @Override
  @SuppressWarnings("checkstyle:MethodParamPad")
  public FunctionCall map(OperationResponse operationResponse) {
    ManageBuyOfferOperationResponse response = (ManageBuyOfferOperationResponse) operationResponse;

    if (response.getSourceAccount() == null) {
      LOG.warn("Source account in ManageOfferOperationResponse is null");
    }

    Asset selling = this.assetMapper.map(response.getSellingAsset());
    Asset buying  = this.assetMapper.map(response.getBuyingAsset());

    String offerId = response.getOfferId().toString();

    return new FunctionCall.Builder()
        .from             (this.fetchAccountId(response))
        .type             (ManageBuyOfferOperation.class.getSimpleName())
        .value            (response.getAmount())
        .lumensTransferred(BigDecimal.ZERO)
        .meta             (this.getOptionalProperties(response, selling, buying))
        .signature        (
            "manage_offer(asset, asset, integer, {numerator, denominator}, unsigned_integer)"
        )
        .arguments        (
            Arrays.asList(
                FunctionCall.Argument.from("selling",  selling.getCode()),
                FunctionCall.Argument.from("buying",   buying.getCode()),
                FunctionCall.Argument.from("amount",   response.getAmount()),
                FunctionCall.Argument.from("price",    response.getPrice()),
                FunctionCall.Argument.from("offer_id", Objects.toString(offerId, ""))
            )
        )
        .build();
  }

  private String fetchAccountId(ManageBuyOfferOperationResponse response) {
    return response.getSourceAccount() != null ? response.getSourceAccount() : "";
  }

  @Override
  public List<Asset> getAssets(OperationResponse operationResponse) {
    ManageBuyOfferOperationResponse response = (ManageBuyOfferOperationResponse) operationResponse;

    Asset selling = this.assetMapper.map(response.getSellingAsset());
    Asset buying  = this.assetMapper.map(response.getBuyingAsset());

    return Arrays.asList(selling, buying);
  }

  private Map<String, Object> getOptionalProperties(
      ManageBuyOfferOperationResponse response,
      Asset                           selling,
      Asset                           buying
  ) {
    Map<String, Object> optionalProperties = new HashMap<>();
    optionalProperties.put("sellingAsset",            selling.getCode());
    optionalProperties.put("stellarSellingAssetType", selling.getType().getName());
    optionalProperties.put("sellingAssetIssuer",      selling.getIssuerAccount());
    optionalProperties.put("buyingAsset",             buying.getCode());
    optionalProperties.put("stellarBuyingAssetType",  buying.getType().getName());
    optionalProperties.put("buyingAssetIssuer",       buying.getIssuerAccount());
    optionalProperties.put("price",                   response.getPrice());
    optionalProperties.put("offerId",                 response.getOfferId());
    return optionalProperties;
  }

}
