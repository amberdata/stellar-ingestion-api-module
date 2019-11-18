package io.amberdata.inbound.stellar.mapper;

import io.amberdata.inbound.domain.Asset;

import org.springframework.stereotype.Component;
import org.stellar.sdk.AssetTypeCreditAlphaNum;

@Component
public class AssetMapper {

  /**
   * Maps a Stellar asset to an Amberdata asset.
   *
   * @param asset the asset to convert
   *
   * @return the converted asset.
   */
  @SuppressWarnings("checkstyle:MethodParamPad")
  public Asset map(org.stellar.sdk.Asset asset) {
    final Asset.AssetType assetType;
    final String          code;
    final String          issuer;

    switch (asset.getType()) {
      case "native":
        assetType = Asset.AssetType.ASSET_TYPE_NATIVE;
        code      = "";
        issuer    = "";
        break;

      case "credit_alphanum4":
        assetType = Asset.AssetType.ASSET_TYPE_CREDIT_ALPHANUM4;
        code      = ((AssetTypeCreditAlphaNum) asset).getCode();
        issuer    = ((AssetTypeCreditAlphaNum) asset).getIssuer();
        break;

      case "credit_alphanum12":
        assetType = Asset.AssetType.ASSET_TYPE_CREDIT_ALPHANUM12;
        code      = ((AssetTypeCreditAlphaNum) asset).getCode();
        issuer    = ((AssetTypeCreditAlphaNum) asset).getIssuer();
        break;

      default:
        assetType = Asset.AssetType.ASSET_TYPE_UNKNOWN;
        code      = "";
        issuer    = "";
        break;
    }

    return new Asset.Builder()
      .type         (assetType)
      .code         (code)
      .issuerAccount(issuer)
      .build        ();
  }

}
