package io.amberdata.domain;

import java.util.Objects;

public class Asset {

    private AssetType type;
    private int code;
    private String issuerAccount;

    public Asset (AssetType type, int code, String issuerAccount) {
        this.type = type;
        this.code = code;
        this.issuerAccount = issuerAccount;
    }

    public AssetType getType () {
        return type;
    }

    public void setType (AssetType type) {
        this.type = type;
    }

    public int getCode () {
        return code;
    }

    public void setCode (int code) {
        this.code = code;
    }

    public String getIssuerAccount () {
        return issuerAccount;
    }

    public void setIssuerAccount (String issuerAccount) {
        this.issuerAccount = issuerAccount;
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Asset asset = (Asset) o;
        return code == asset.code &&
            type == asset.type &&
            Objects.equals(issuerAccount, asset.issuerAccount);
    }

    @Override
    public int hashCode () {
        return Objects.hash(type, code, issuerAccount);
    }

    @Override
    public String toString () {
        return "Asset{" +
            "type=" + type +
            ", code=" + code +
            ", issuerAccount='" + issuerAccount + '\'' +
            '}';
    }

    public enum AssetType {
        ASSET_TYPE_NATIVE(0),
        ASSET_TYPE_CREDIT_ALPHANUM4(1),
        ASSET_TYPE_CREDIT_ALPHANUM12(2),
        ASSET_TYPE_CREDIT_ALPHANUM(3);

        private int value;

        private AssetType (int value) {
            this.value = value;
        }

        public int getValue () {
            return this.value;
        }
    }
}