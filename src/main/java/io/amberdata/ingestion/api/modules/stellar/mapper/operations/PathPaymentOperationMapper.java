package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PathPaymentOperationResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.amberdata.domain.Asset;
import io.amberdata.domain.FunctionCall;
import io.amberdata.ingestion.api.modules.stellar.mapper.AssetMapper;

public class PathPaymentOperationMapper implements OperationMapper {

    private AssetMapper assetMapper;

    public PathPaymentOperationMapper (AssetMapper assetMapper) {
        this.assetMapper = assetMapper;
    }

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        PathPaymentOperationResponse response = (PathPaymentOperationResponse) operationResponse;

        Asset asset = assetMapper.map(response.getAsset());

        return new FunctionCall.Builder()
            .from(response.getFrom().getAccountId())
            .to(response.getTo().getAccountId())
            .assetType(asset.getCode())
            .value(response.getAmount())
            .meta(getMetaProperties(response, asset))
            .build();
    }

    @Override
    public List<Asset> getAssets (OperationResponse operationResponse) {
        PathPaymentOperationResponse response = (PathPaymentOperationResponse) operationResponse;

        Asset asset       = assetMapper.map(response.getAsset());
        Asset sourceAsset = assetMapper.map(response.getSourceAsset());
        return Arrays.asList(asset, sourceAsset);
    }

    private String getMetaProperties (PathPaymentOperationResponse response, Asset asset) {
        // commented out due to a bug in sdk (causes npe - no code for native asset)
        // Asset sourceAsset = assetMapper.map(response.getSourceAsset());

        Map<String, String> metaMap = new HashMap<>();
        metaMap.put("stellarAssetType", asset.getType().getName());
        metaMap.put("assetIssuer", asset.getIssuerAccount());

        // commented out due to a bug in sdk (causes npe - no code for native asset)
        // metaMap.put("sourceAsset", sourceAsset.getCode());
        // metaMap.put("stellarSourceAssetType", sourceAsset.getType().getName());
        // metaMap.put("sourceAssetIssuer", sourceAsset.getIssuerAccount());
        metaMap.put("sourceMax", response.getSourceMax());
        try {
            return new ObjectMapper().writeValueAsString(metaMap);
        }
        catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
