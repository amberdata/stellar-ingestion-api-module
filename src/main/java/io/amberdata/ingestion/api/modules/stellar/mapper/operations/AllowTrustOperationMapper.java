package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.HashMap;
import java.util.Map;

import org.stellar.sdk.responses.operations.AllowTrustOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.amberdata.domain.Asset;
import io.amberdata.domain.FunctionCall;
import io.amberdata.ingestion.api.modules.stellar.mapper.AssetMapper;

public class AllowTrustOperationMapper implements OperationMapper {

    private AssetMapper assetMapper;

    public AllowTrustOperationMapper (AssetMapper assetMapper) {
        this.assetMapper = assetMapper;
    }

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        AllowTrustOperationResponse response = (AllowTrustOperationResponse) operationResponse;

        Asset asset = assetMapper.map(response.getAsset());

        return new FunctionCall.Builder()
            .from(response.getTrustee().getAccountId())
            .to(response.getTrustor().getAccountId())
            .assetType(asset.getCode())
            .meta(getMetaProperties(response))
            .build();
    }

    private String getMetaProperties (AllowTrustOperationResponse response) {
        Map<String, String> metaMap = new HashMap<>();
        metaMap.put("isAuthorize", String.valueOf(response.isAuthorize()));
        try {
            return new ObjectMapper().writeValueAsString(metaMap);
        }
        catch (JsonProcessingException e) {
            return null;
        }
    }
}
