package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.HashMap;
import java.util.Map;

import org.stellar.sdk.responses.operations.AllowTrustOperationResponse;
import org.stellar.sdk.responses.operations.CreatePassiveOfferOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.amberdata.domain.Asset;
import io.amberdata.domain.FunctionCall;
import io.amberdata.domain.operations.CreatePassiveOfferOperation;
import io.amberdata.domain.operations.Operation;
import io.amberdata.ingestion.api.modules.stellar.mapper.AssetMapper;

public class CreatePassiveOfferOperationMapper implements OperationMapper {

    private AssetMapper assetMapper;

    public CreatePassiveOfferOperationMapper (AssetMapper assetMapper) {
        this.assetMapper = assetMapper;
    }

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        CreatePassiveOfferOperationResponse response = (CreatePassiveOfferOperationResponse) operationResponse;

        return new FunctionCall.Builder()
            .from(response.getSourceAccount().getAccountId())
            .value(response.getAmount())
            .meta(getMetaProperties(response))
            .build();
    }

    private String getMetaProperties (CreatePassiveOfferOperationResponse response) {
        Asset selling = assetMapper.map(response.getSellingAsset());
        Asset buying = assetMapper.map(response.getBuyingAsset());

        Map<String, String> metaMap = new HashMap<>();
        metaMap.put("sellingAsset", selling.getCode());
        metaMap.put("buyingAsset", buying.getCode());
        metaMap.put("price", response.getPrice());
        try {
            return new ObjectMapper().writeValueAsString(metaMap);
        }
        catch (JsonProcessingException e) {
            return null;
        }
    }
}
