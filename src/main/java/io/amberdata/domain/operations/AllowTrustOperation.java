package io.amberdata.domain.operations;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import io.amberdata.domain.Asset;

public class AllowTrustOperation extends Operation {

    private String trustor;
    private Asset asset;
    private boolean authorize;

    public AllowTrustOperation (String sourceAccount,
                                String trustor,
                                Asset asset,
                                boolean authorize) {
        super(sourceAccount);
        this.trustor = trustor;
        this.asset = asset;
        this.authorize = authorize;
    }

    public String getTrustor () {
        return trustor;
    }

    public void setTrustor (String trustor) {
        this.trustor = trustor;
    }

    public Asset getAsset () {
        return asset;
    }

    public void setAsset (Asset asset) {
        this.asset = asset;
    }

    public boolean isAuthorize () {
        return authorize;
    }

    public void setAuthorize (boolean authorize) {
        this.authorize = authorize;
    }

    @Override
    public List<String> getInvolvedAccounts () {
        return Arrays.asList(getSourceAccount(), this.trustor);
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AllowTrustOperation that = (AllowTrustOperation) o;
        return authorize == that.authorize &&
            Objects.equals(trustor, that.trustor) &&
            Objects.equals(asset, that.asset);
    }

    @Override
    public int hashCode () {
        return Objects.hash(trustor, asset, authorize);
    }

    @Override
    public String toString () {
        return "AllowTrustOperation{" +
            "trustor='" + trustor + '\'' +
            ", asset=" + asset +
            ", authorize=" + authorize +
            '}';
    }
}
