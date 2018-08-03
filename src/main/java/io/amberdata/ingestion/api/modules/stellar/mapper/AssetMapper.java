package io.amberdata.ingestion.api.modules.stellar.mapper;

import org.springframework.stereotype.Component;
import org.stellar.sdk.AssetTypeCreditAlphaNum;

import io.amberdata.domain.Asset;

@Component
public class AssetMapper {

    public Asset map (org.stellar.sdk.Asset asset) {
        Asset.AssetType assetType;
        String          code;
        String          issuer;

        switch (asset.getType()) {
            case "native":
                assetType = Asset.AssetType.ASSET_TYPE_NATIVE;
                code = null;
                issuer = null;
                break;
            case "credit_alphanum4":
                assetType = Asset.AssetType.ASSET_TYPE_CREDIT_ALPHANUM4;
                code = ((AssetTypeCreditAlphaNum) asset).getCode();
                issuer = ((AssetTypeCreditAlphaNum) asset).getIssuer().getAccountId();
                break;
            case "credit_alphanum12":
                assetType = Asset.AssetType.ASSET_TYPE_CREDIT_ALPHANUM12;
                code = ((AssetTypeCreditAlphaNum) asset).getCode();
                issuer = ((AssetTypeCreditAlphaNum) asset).getIssuer().getAccountId();
                break;
            default:
                assetType = Asset.AssetType.ASSET_TYPE_UNKNOWN;
                code = null;
                issuer = null;
        }

        return new Asset(assetType, code, issuer);
    }
}
