package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stellar.sdk.responses.operations.ChangeTrustOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.amberdata.domain.Asset;
import io.amberdata.domain.FunctionCall;
import io.amberdata.ingestion.api.modules.stellar.mapper.AssetMapper;

public class ChangeTrustOperationMapper implements OperationMapper {

    private static final Logger LOG = LoggerFactory.getLogger(ChangeTrustOperationMapper.class);

    private AssetMapper assetMapper;

    public ChangeTrustOperationMapper (AssetMapper assetMapper) {
        this.assetMapper = assetMapper;
    }

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        ChangeTrustOperationResponse response = (ChangeTrustOperationResponse) operationResponse;

        if (response.getAsset() == null) {
            LOG.warn("Asset in ChangeTrustOperationResponse is null");
        }

        if (response.getTrustor() == null) {
            LOG.warn("Trustor account in ChangeTrustOperationResponse is null");
        }

        if (response.getTrustee() == null) {
            LOG.warn("Trustee account in ChangeTrustOperationResponse is null");
        }

        Asset asset = assetMapper.map(response.getAsset());

        return new FunctionCall.Builder()
            .from(response.getTrustor() != null ? response.getTrustor().getAccountId() : "")
            .to(response.getTrustee() != null ? response.getTrustee().getAccountId() : "")
            .assetType(asset.getCode())
            .optionalProperties(getOptionalProperties(response, asset))
            .build();
    }

    @Override
    public List<Asset> getAssets (OperationResponse operationResponse) {
        ChangeTrustOperationResponse response = (ChangeTrustOperationResponse) operationResponse;

        Asset asset = assetMapper.map(response.getAsset());
        return Collections.singletonList(asset);
    }

    private Map<String, Object> getOptionalProperties (ChangeTrustOperationResponse response, Asset asset) {
        Map<String, Object> optionalProperties = new HashMap<>();
        optionalProperties.put("limit", response.getLimit());
        optionalProperties.put("stellarAssetType", asset.getType().getName());
        optionalProperties.put("assetIssuer", asset.getIssuerAccount());
        return optionalProperties;
    }
}
