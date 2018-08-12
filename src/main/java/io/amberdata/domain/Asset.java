package io.amberdata.domain;

import java.util.Map;
import java.util.Objects;

public class Asset implements BlockchainEntity {
    private AssetType type;
    private String    code;
    private String    issuerAccount;
    private String    amount;

    private Map<String, Object> optionalProperties;

    public Asset () {
    }

    private Asset (Builder builder) {
        this.type = builder.type;
        this.code = builder.code;
        this.issuerAccount = builder.issuerAccount;
        this.amount = builder.amount;
        this.optionalProperties = builder.optionalProperties;
    }

    public AssetType getType () {
        return type;
    }

    public void setType (AssetType type) {
        this.type = type;
    }

    public String getCode () {
        return code;
    }

    public void setCode (String code) {
        this.code = code;
    }

    public String getIssuerAccount () {
        return issuerAccount;
    }

    public void setIssuerAccount (String issuerAccount) {
        this.issuerAccount = issuerAccount;
    }

    public String getAmount () {
        return amount;
    }

    public void setAmount (String amount) {
        this.amount = amount;
    }

    public Map<String, Object> getOptionalProperties () {
        return optionalProperties;
    }

    public void setOptionalProperties (Map<String, Object> optionalProperties) {
        this.optionalProperties = optionalProperties;
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
        return type == asset.type &&
            Objects.equals(code, asset.code) &&
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

    public static class Builder {
        private AssetType type;
        private String    code;
        private String    issuerAccount;
        private String    amount;

        private Map<String, Object> optionalProperties;

        public Builder type (AssetType value) {
            this.type = value;
            return this;
        }

        public Builder code (String value) {
            this.code = value;
            return this;
        }

        public Builder issuerAccount (String value) {
            this.issuerAccount = value;
            return this;
        }

        public Builder amount (String value) {
            this.amount = value;
            return this;
        }

        public Builder optionalProperties (Map<String, Object> value) {
            this.optionalProperties = value;
            return this;
        }

        public Asset build () {
            return new Asset(this);
        }
    }

    public enum AssetType {
        ASSET_TYPE_NATIVE(0, "native"),
        ASSET_TYPE_CREDIT_ALPHANUM4(1, "credit_alphanum4"),
        ASSET_TYPE_CREDIT_ALPHANUM12(2, "credit_alphanum12"),
        ASSET_TYPE_UNKNOWN(3, "unknown");

        private int    value;
        private String name;

        AssetType (int value, String name) {
            this.value = value;
            this.name = name;
        }

        public static AssetType fromName (String name) {
            for (AssetType assetType : values()) {
                if (assetType.getName().equals(name)) {
                    return assetType;
                }
            }

            return AssetType.ASSET_TYPE_UNKNOWN;
        }

        public int getValue () {
            return this.value;
        }

        public String getName () {
            return name;
        }
    }
}
