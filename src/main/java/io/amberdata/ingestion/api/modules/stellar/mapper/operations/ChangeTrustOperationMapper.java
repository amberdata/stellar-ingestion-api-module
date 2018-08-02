package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.HashMap;
import java.util.Map;

import org.stellar.sdk.responses.operations.AllowTrustOperationResponse;
import org.stellar.sdk.responses.operations.ChangeTrustOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.amberdata.domain.Asset;
import io.amberdata.domain.FunctionCall;
import io.amberdata.domain.operations.ChangeTrustOperation;
import io.amberdata.domain.operations.Operation;
import io.amberdata.ingestion.api.modules.stellar.mapper.AssetMapper;

public class ChangeTrustOperationMapper implements OperationMapper {

    private AssetMapper assetMapper;

    public ChangeTrustOperationMapper (AssetMapper assetMapper) {
        this.assetMapper = assetMapper;
    }

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        ChangeTrustOperationResponse response = (ChangeTrustOperationResponse) operationResponse;

        Asset asset = assetMapper.map(response.getAsset());

        return new FunctionCall.Builder()
            .from(response.getTrustor().getAccountId())
            .to(response.getTrustee().getAccountId())
            .assetType(asset.getCode())
            .meta(getMetaProperties(response))
            .build();
    }

    private String getMetaProperties (ChangeTrustOperationResponse response) {
        Map<String, String> metaMap = new HashMap<>();
        metaMap.put("limit", response.getLimit());
        try {
            return new ObjectMapper().writeValueAsString(metaMap);
        }
        catch (JsonProcessingException e) {
            return null;
        }
    }
}
