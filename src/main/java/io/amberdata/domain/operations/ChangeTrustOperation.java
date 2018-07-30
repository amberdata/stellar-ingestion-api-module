package io.amberdata.domain.operations;

import java.util.Objects;

import io.amberdata.domain.Asset;

public class ChangeTrustOperation extends Operation {

    private Asset  asset;
    private String limit;

    public ChangeTrustOperation (String sourceAccount,
                                 Asset asset,
                                 String limit) {
        super(sourceAccount);
        this.asset = asset;
        this.limit = limit;
    }

    public Asset getAsset () {
        return asset;
    }

    public void setAsset (Asset asset) {
        this.asset = asset;
    }

    public String getLimit () {
        return limit;
    }

    public void setLimit (String limit) {
        this.limit = limit;
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChangeTrustOperation that = (ChangeTrustOperation) o;
        return Objects.equals(asset, that.asset) &&
            Objects.equals(limit, that.limit);
    }

    @Override
    public int hashCode () {
        return Objects.hash(asset, limit);
    }

    @Override
    public String toString () {
        return "ChangeTrustOperation{" +
            "asset=" + asset +
            ", limit='" + limit + '\'' +
            '}';
    }
}
