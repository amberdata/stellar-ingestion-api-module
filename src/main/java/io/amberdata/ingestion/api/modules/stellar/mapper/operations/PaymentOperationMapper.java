package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

import io.amberdata.domain.Asset;
import io.amberdata.domain.FunctionCall;
import io.amberdata.ingestion.api.modules.stellar.mapper.AssetMapper;

public class PaymentOperationMapper implements OperationMapper {

    private AssetMapper assetMapper;

    public PaymentOperationMapper (AssetMapper assetMapper) {
        this.assetMapper = assetMapper;
    }

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        PaymentOperationResponse response = (PaymentOperationResponse) operationResponse;

        Preconditions.checkNotNull(response.getAsset(), "Asset in PaymentOperationResponse is null");
        Preconditions.checkNotNull(response.getFrom(), "Source account in PaymentOperationResponse is null");
        Preconditions.checkNotNull(response.getTo(), "Destination account in PaymentOperationResponse is null");

        Asset asset = this.assetMapper.map(response.getAsset());

        return new FunctionCall.Builder()
            .from(response.getFrom() != null ? response.getFrom().getAccountId() : null)
            .to(response.getTo() != null ? response.getTo().getAccountId() : null)
            .assetType(asset.getCode())
            .value(response.getAmount())
            .meta(getMetaProperties(asset))
            .build();
    }

    @Override
    public List<Asset> getAssets (OperationResponse operationResponse) {
        PaymentOperationResponse response = (PaymentOperationResponse) operationResponse;

        Asset asset = assetMapper.map(response.getAsset());
        return Collections.singletonList(asset);
    }

    private String getMetaProperties (Asset asset) {
        Map<String, String> metaMap = new HashMap<>();
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
