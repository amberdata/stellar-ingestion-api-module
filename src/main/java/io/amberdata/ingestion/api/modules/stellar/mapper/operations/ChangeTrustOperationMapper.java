package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.stellar.sdk.responses.operations.ChangeTrustOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

import io.amberdata.domain.Asset;
import io.amberdata.domain.FunctionCall;
import io.amberdata.ingestion.api.modules.stellar.mapper.AssetMapper;

public class ChangeTrustOperationMapper implements OperationMapper {

    private AssetMapper assetMapper;

    public ChangeTrustOperationMapper (AssetMapper assetMapper) {
        this.assetMapper = assetMapper;
    }

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        ChangeTrustOperationResponse response = (ChangeTrustOperationResponse) operationResponse;

        Preconditions.checkNotNull(response.getAsset(), "Asset in ChangeTrustOperationResponse is null");
        Preconditions.checkNotNull(response.getTrustor(), "Trustor account in ChangeTrustOperationResponse is null");
        Preconditions.checkNotNull(response.getTrustee(), "Trustee account in ChangeTrustOperationResponse is null");

        Asset asset = assetMapper.map(response.getAsset());

        return new FunctionCall.Builder()
            .from(response.getTrustor() != null ? response.getTrustor().getAccountId() : null)
            .to(response.getTrustee() != null ? response.getTrustee().getAccountId() : null)
            .assetType(asset.getCode())
            .meta(getMetaProperties(response, asset))
            .build();
    }

    @Override
    public List<Asset> getAssets (OperationResponse operationResponse) {
        ChangeTrustOperationResponse response = (ChangeTrustOperationResponse) operationResponse;

        Asset asset = assetMapper.map(response.getAsset());
        return Collections.singletonList(asset);
    }

    private String getMetaProperties (ChangeTrustOperationResponse response, Asset asset) {
        Map<String, String> metaMap = new HashMap<>();
        metaMap.put("limit", response.getLimit());
        metaMap.put("stellarAssetType", asset.getType().getName());
        metaMap.put("assetIssuer", asset.getIssuerAccount());
        try {
            return new ObjectMapper().writeValueAsString(metaMap);
        }
        catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
