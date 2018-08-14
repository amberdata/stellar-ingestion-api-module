package io.amberdata.ingestion.stellar.mapper.operations;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stellar.sdk.AllowTrustOperation;
import org.stellar.sdk.responses.operations.AllowTrustOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.ingestion.domain.Asset;
import io.amberdata.ingestion.domain.FunctionCall;
import io.amberdata.ingestion.stellar.mapper.AssetMapper;

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
            .type(AllowTrustOperation.class.getSimpleName())
            .assetType(asset.getCode())
            .optionalProperties(getOptionalProperties(response, asset))
            .signature("allow_trust(account_id, asset, boolean)")
            .arguments(Arrays.asList(
                    FunctionCall.Argument.from("trustor", response.getTrustor().getAccountId()),
                    FunctionCall.Argument.from("type", asset.getCode()),
                    FunctionCall.Argument.from("authorize", String.valueOf(response.isAuthorize()))
                )
            )
            .build();
    }

    @Override
    public List<Asset> getAssets (OperationResponse operationResponse) {
        AllowTrustOperationResponse response = (AllowTrustOperationResponse) operationResponse;

        Asset asset = assetMapper.map(response.getAsset());
        return Collections.singletonList(asset);
    }

    private Map<String, Object> getOptionalProperties (AllowTrustOperationResponse response, Asset asset) {
        Map<String, Object> optionalProperties = new HashMap<>();
        optionalProperties.put("isAuthorize", String.valueOf(response.isAuthorize()));
        optionalProperties.put("stellarAssetType", asset.getType().getName());
        optionalProperties.put("assetIssuer", asset.getIssuerAccount());
        return optionalProperties;
    }
}
