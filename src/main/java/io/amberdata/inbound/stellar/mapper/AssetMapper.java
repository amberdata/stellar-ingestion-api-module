package io.amberdata.inbound.stellar.mapper;

import org.springframework.stereotype.Component;
import org.stellar.sdk.AssetTypeCreditAlphaNum;

import io.amberdata.inbound.domain.Asset;

@Component
public class AssetMapper {
    public Asset map (org.stellar.sdk.Asset asset) {
        final Asset.AssetType assetType;
        final String          code;
        final String          issuer;

        switch (asset.getType()) {
            case "native":
                assetType = Asset.AssetType.ASSET_TYPE_NATIVE;
                code = "";
                issuer = "";
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
                code = "";
                issuer = "";
                break;
        }

        return new Asset.Builder()
            .type(assetType)
            .code(code)
            .issuerAccount(issuer)
            .build();
    }
}
