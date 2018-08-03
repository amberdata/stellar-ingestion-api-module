package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stellar.sdk.responses.operations.AllowTrustOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.amberdata.domain.Asset;
import io.amberdata.domain.FunctionCall;
import io.amberdata.ingestion.api.modules.stellar.mapper.AssetMapper;

public class AllowTrustOperationMapper implements OperationMapper {

    private static final Logger LOG = LoggerFactory.getLogger(AllowTrustOperationMapper.class);

    private AssetMapper assetMapper;

    public AllowTrustOperationMapper (AssetMapper assetMapper) {
        this.assetMapper = assetMapper;
    }

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        AllowTrustOperationResponse response = (AllowTrustOperationResponse) operationResponse;

        if (response.getAsset() == null) {
            LOG.warn("Asset in AllowTrustOperationResponse is null");
        }

        if (response.getTrustee() == null) {
            LOG.warn("Trustee account in AllowTrustOperationResponse is null");
        }

        if (response.getTrustor() == null) {
            LOG.warn("Trustor account in AllowTrustOperationResponse is null");
        }

        Asset asset = assetMapper.map(response.getAsset());

        return new FunctionCall.Builder()
            .from(response.getTrustee() != null ? response.getTrustee().getAccountId() : "")
            .to(response.getTrustor() != null ? response.getTrustor().getAccountId() : "")
            .assetType(asset.getCode())
            .meta(getMetaProperties(response, asset))
            .build();
    }

    @Override
    public List<Asset> getAssets (OperationResponse operationResponse) {
        AllowTrustOperationResponse response = (AllowTrustOperationResponse) operationResponse;

        Asset asset = assetMapper.map(response.getAsset());
        return Collections.singletonList(asset);
    }

    private String getMetaProperties (AllowTrustOperationResponse response, Asset asset) {
        Map<String, String> metaMap = new HashMap<>();
        metaMap.put("isAuthorize", String.valueOf(response.isAuthorize()));
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
