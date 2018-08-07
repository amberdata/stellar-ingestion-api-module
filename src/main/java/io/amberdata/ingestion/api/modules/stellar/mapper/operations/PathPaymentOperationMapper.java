package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PathPaymentOperationResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.amberdata.domain.Asset;
import io.amberdata.domain.FunctionCall;
import io.amberdata.ingestion.api.modules.stellar.mapper.AssetMapper;

public class PathPaymentOperationMapper implements OperationMapper {

    private static final Logger LOG = LoggerFactory.getLogger(PathPaymentOperationMapper.class);

    private AssetMapper assetMapper;

    public PathPaymentOperationMapper (AssetMapper assetMapper) {
        this.assetMapper = assetMapper;
    }

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        PathPaymentOperationResponse response = (PathPaymentOperationResponse) operationResponse;

        if (response.getAsset() == null) {
            LOG.warn("Asset in PathPaymentOperationResponse is null");
        }

        if (response.getFrom() == null) {
            LOG.warn("Source account in PathPaymentOperationResponse is null");
        }

        if (response.getTo() == null) {
            LOG.warn("Destination account in PathPaymentOperationResponse is null");
        }

        Asset asset = assetMapper.map(response.getAsset());

        return new FunctionCall.Builder()
            .from(response.getFrom() != null ? response.getFrom().getAccountId() : "")
            .to(response.getTo() != null ? response.getTo().getAccountId() : "")
            .assetType(asset.getCode())
            .value(response.getAmount())
            .optionalProperties(getOptionalProperties(response, asset))
            .build();
    }

    @Override
    public List<Asset> getAssets (OperationResponse operationResponse) {
        PathPaymentOperationResponse response = (PathPaymentOperationResponse) operationResponse;

        Asset asset       = assetMapper.map(response.getAsset());
        Asset sourceAsset = assetMapper.map(response.getSourceAsset());
        return Arrays.asList(asset, sourceAsset);
    }

    private Map<String, Object> getOptionalProperties (PathPaymentOperationResponse response, Asset asset) {
        // commented out due to a bug in sdk (causes npe - no code for native asset)
        // Asset sourceAsset = assetMapper.map(response.getSourceAsset());

        Map<String, Object> optionalProperties = new HashMap<>();
        optionalProperties.put("stellarAssetType", asset.getType().getName());
        optionalProperties.put("assetIssuer", asset.getIssuerAccount());

        // commented out due to a bug in sdk (causes npe - no code for native asset)
        // metaMap.put("sourceAsset", sourceAsset.getCode());
        // metaMap.put("stellarSourceAssetType", sourceAsset.getType().getName());
        // metaMap.put("sourceAssetIssuer", sourceAsset.getIssuerAccount());
        optionalProperties.put("sourceMax", response.getSourceMax());
        return optionalProperties;
    }
}
