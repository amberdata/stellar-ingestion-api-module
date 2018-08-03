package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stellar.sdk.responses.operations.ManageOfferOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.amberdata.domain.Asset;
import io.amberdata.domain.FunctionCall;
import io.amberdata.ingestion.api.modules.stellar.mapper.AssetMapper;

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

        return new FunctionCall.Builder()
            .from(response.getSourceAccount() != null ? response.getSourceAccount().getAccountId() : "")
            .value(response.getAmount())
            .meta(getMetaProperties(response))
            .build();
    }

    @Override
    public List<Asset> getAssets (OperationResponse operationResponse) {
        ManageOfferOperationResponse response = (ManageOfferOperationResponse) operationResponse;

        Asset selling = assetMapper.map(response.getSellingAsset());
        Asset buying = assetMapper.map(response.getBuyingAsset());
        return Arrays.asList(selling, buying);
    }

    private String getMetaProperties (ManageOfferOperationResponse response) {
        Asset selling = assetMapper.map(response.getSellingAsset());
        Asset buying  = assetMapper.map(response.getBuyingAsset());

        Map<String, String> metaMap = new HashMap<>();
        metaMap.put("sellingAsset", selling.getCode());
        metaMap.put("stellarSellingAssetType", selling.getType().getName());
        metaMap.put("sellingAssetIssuer", selling.getIssuerAccount());
        metaMap.put("buyingAsset", buying.getCode());
        metaMap.put("stellarBuyingAssetType", buying.getType().getName());
        metaMap.put("buyingAssetIssuer", buying.getIssuerAccount());
        metaMap.put("price", response.getPrice());
        metaMap.put("offerId", response.getOfferId().toString());
        try {
            return new ObjectMapper().writeValueAsString(metaMap);
        }
        catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
