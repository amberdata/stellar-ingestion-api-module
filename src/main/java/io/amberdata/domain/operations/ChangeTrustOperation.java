package io.amberdata.domain.operations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.amberdata.domain.Asset;

public class ChangeTrustOperation extends Operation {

    private String trusteeAccount;
    private Asset  asset;
    private String limit;

    public ChangeTrustOperation (String sourceAccount,
                                 String trusteeAccount,
                                 Asset asset,
                                 String limit) {
        super(sourceAccount);
        this.trusteeAccount = trusteeAccount;
        this.asset = asset;
        this.limit = limit;
    }

    public String getTrusteeAccount () {
        return trusteeAccount;
    }

    public void setTrusteeAccount (String trusteeAccount) {
        this.trusteeAccount = trusteeAccount;
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
    public List<String> getInvolvedAccounts () {
        return Arrays.asList(getSourceAccount(), this.trusteeAccount);
    }

    @Override
    public List<Asset> getInvolvedAssets () {
        return Collections.singletonList(asset);
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
        return Objects.equals(trusteeAccount, that.trusteeAccount) &&
            Objects.equals(asset, that.asset) &&
            Objects.equals(limit, that.limit);
    }

    @Override
    public int hashCode () {
        return Objects.hash(trusteeAccount, asset, limit);
    }

    @Override
    public String toString () {
        return "ChangeTrustOperation{" +
            "trusteeAccount='" + trusteeAccount + '\'' +
            ", asset=" + asset +
            ", limit='" + limit + '\'' +
            '}';
    }
}
