package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.stellar.sdk.responses.operations.CreatePassiveOfferOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PathPaymentOperationResponse;
import org.stellar.sdk.xdr.PathPaymentOp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.amberdata.domain.Asset;
import io.amberdata.domain.FunctionCall;
import io.amberdata.domain.operations.Operation;
import io.amberdata.domain.operations.PathPaymentOperation;
import io.amberdata.ingestion.api.modules.stellar.mapper.AssetMapper;

public class PathPaymentOperationMapper implements OperationMapper {

    private AssetMapper assetMapper;

    public PathPaymentOperationMapper (AssetMapper assetMapper) {
        this.assetMapper = assetMapper;
    }

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        PathPaymentOperationResponse response = (PathPaymentOperationResponse) operationResponse;

        Asset asset  = assetMapper.map(response.getAsset());

        return new FunctionCall.Builder()
            .from(response.getFrom().getAccountId())
            .to(response.getTo().getAccountId())
            .assetType(asset.getCode())
            .value(response.getAmount())
            .meta(getMetaProperties(response))
            .build();
    }

    private String getMetaProperties (PathPaymentOperationResponse response) {
        Asset sourceAsset = assetMapper.map(response.getSourceAsset());

        Map<String, String> metaMap = new HashMap<>();
        metaMap.put("sourceAsset", sourceAsset.getCode());
        metaMap.put("sourceMax", response.getSourceMax());
        try {
            return new ObjectMapper().writeValueAsString(metaMap);
        }
        catch (JsonProcessingException e) {
            return null;
        }
    }
}
