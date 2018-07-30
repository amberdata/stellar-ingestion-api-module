package io.amberdata.domain.operations;

import java.util.Objects;

public class AllowTrustOperation extends Operation {

    private String trustor;
    private String assetCode;
    private boolean authorize;

    public AllowTrustOperation (String sourceAccount,
                                String trustor,
                                String assetCode,
                                boolean authorize) {
        super(sourceAccount);
        this.trustor = trustor;
        this.assetCode = assetCode;
        this.authorize = authorize;
    }

    public String getTrustor () {
        return trustor;
    }

    public void setTrustor (String trustor) {
        this.trustor = trustor;
    }

    public String getAssetCode () {
        return assetCode;
    }

    public void setAssetCode (String assetCode) {
        this.assetCode = assetCode;
    }

    public boolean isAuthorize () {
        return authorize;
    }

    public void setAuthorize (boolean authorize) {
        this.authorize = authorize;
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
            Objects.equals(assetCode, that.assetCode);
    }

    @Override
    public int hashCode () {
        return Objects.hash(trustor, assetCode, authorize);
    }

    @Override
    public String toString () {
        return "AllowTrustOperation{" +
            "trustor='" + trustor + '\'' +
            ", assetCode='" + assetCode + '\'' +
            ", authorize=" + authorize +
            '}';
    }
}
