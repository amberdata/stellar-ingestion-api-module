package io.amberdata.ingestion.stellar.mapper.operations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stellar.sdk.ManageOfferOperation;
import org.stellar.sdk.responses.operations.ManageOfferOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.ingestion.domain.Asset;
import io.amberdata.ingestion.domain.FunctionCall;
import io.amberdata.ingestion.stellar.mapper.AssetMapper;

public class ManageOfferOperationMapper implements OperationMapper {

    private static final Logger LOG = LoggerFactory.getLogger(ManageOfferOperationMapper.class);

    private AssetMapper assetMapper;

    public ManageOfferOperationMapper (AssetMapper assetMapper) {
        this.assetMapper = assetMapper;
    }

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        ManageOfferOperationResponse response = (ManageOfferOperationResponse) operationResponse;

        if (response.getSourceAccount() == null) {
            LOG.warn("Source account in ManageOfferOperationResponse is null");
        }

        Asset selling = assetMapper.map(response.getSellingAsset());
        Asset buying  = assetMapper.map(response.getBuyingAsset());

        return new FunctionCall.Builder()
            .from(fetchAccountId(response))
            .type(ManageOfferOperation.class.getSimpleName())
            .value(response.getAmount())
            .meta(getOptionalProperties(response, selling, buying))
            .signature("manage_offer(asset, asset, integer, {numerator, denominator}, unsigned_integer)")
            .arguments(
                Arrays.asList(
                    FunctionCall.Argument.from("selling", selling.getCode()),
                    FunctionCall.Argument.from("buying", buying.getCode()),
                    FunctionCall.Argument.from("amount", response.getAmount()),
                    FunctionCall.Argument.from("price", response.getPrice()),
                    FunctionCall.Argument.from("offer_id", Objects.toString(response.getOfferId(), ""))
                )
            )
            .build();
    }

    private String fetchAccountId (ManageOfferOperationResponse response) {
        return response.getSourceAccount() != null ? response.getSourceAccount().getAccountId() : "";
    }

    @Override
    public List<Asset> getAssets (OperationResponse operationResponse) {
        ManageOfferOperationResponse response = (ManageOfferOperationResponse) operationResponse;

        Asset selling = assetMapper.map(response.getSellingAsset());
        Asset buying = assetMapper.map(response.getBuyingAsset());
        return Arrays.asList(selling, buying);
    }

    private Map<String, Object> getOptionalProperties (ManageOfferOperationResponse response,
                                                       Asset selling,
                                                       Asset buying) {
        Map<String, Object> optionalProperties = new HashMap<>();
        optionalProperties.put("sellingAsset", selling.getCode());
        optionalProperties.put("stellarSellingAssetType", selling.getType().getName());
        optionalProperties.put("sellingAssetIssuer", selling.getIssuerAccount());
        optionalProperties.put("buyingAsset", buying.getCode());
        optionalProperties.put("stellarBuyingAssetType", buying.getType().getName());
        optionalProperties.put("buyingAssetIssuer", buying.getIssuerAccount());
        optionalProperties.put("price", response.getPrice());
        optionalProperties.put("offerId", response.getOfferId());
        return optionalProperties;
    }
}
