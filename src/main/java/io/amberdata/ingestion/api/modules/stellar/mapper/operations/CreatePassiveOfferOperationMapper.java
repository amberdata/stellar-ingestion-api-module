package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stellar.sdk.CreatePassiveOfferOperation;
import org.stellar.sdk.responses.operations.CreatePassiveOfferOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.domain.Asset;
import io.amberdata.domain.FunctionCall;
import io.amberdata.ingestion.api.modules.stellar.mapper.AssetMapper;

public class CreatePassiveOfferOperationMapper implements OperationMapper {

    private static final Logger LOG = LoggerFactory.getLogger(CreatePassiveOfferOperationMapper.class);

    private AssetMapper assetMapper;

    public CreatePassiveOfferOperationMapper (AssetMapper assetMapper) {
        this.assetMapper = assetMapper;
    }

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        CreatePassiveOfferOperationResponse response = (CreatePassiveOfferOperationResponse) operationResponse;

        if (response.getSourceAccount() == null) {
            LOG.warn("Source account in CreatePassiveOfferOperationResponse is null");
        }

        return new FunctionCall.Builder()
            .from(response.getSourceAccount() != null ? response.getSourceAccount().getAccountId() : "")
            .type(CreatePassiveOfferOperation.class.getSimpleName())
            .value(response.getAmount())
            .optionalProperties(getOptionalProperties(response))
            .build();
    }

    @Override
    public List<Asset> getAssets (OperationResponse operationResponse) {
        CreatePassiveOfferOperationResponse response = (CreatePassiveOfferOperationResponse) operationResponse;

        Asset selling = assetMapper.map(response.getSellingAsset());
        Asset buying = assetMapper.map(response.getBuyingAsset());
        return Arrays.asList(selling, buying);
    }

    private Map<String, Object> getOptionalProperties (CreatePassiveOfferOperationResponse response) {
        Asset selling = assetMapper.map(response.getSellingAsset());
        Asset buying = assetMapper.map(response.getBuyingAsset());

        Map<String, Object> optionalProperties = new HashMap<>();
        optionalProperties.put("sellingAsset", selling.getCode());
        optionalProperties.put("stellarSellingAssetType", selling.getType().getName());
        optionalProperties.put("sellingAssetIssuer", selling.getIssuerAccount());
        optionalProperties.put("buyingAsset", buying.getCode());
        optionalProperties.put("stellarBuyingAssetType", buying.getType().getName());
        optionalProperties.put("buyingAssetIssuer", buying.getIssuerAccount());
        optionalProperties.put("price", response.getPrice());
        return optionalProperties;
    }
}
