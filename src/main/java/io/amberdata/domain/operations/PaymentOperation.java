package io.amberdata.domain.operations;

import java.util.Objects;

import io.amberdata.domain.Asset;

public class PaymentOperation extends Operation {

    private String destinationAccount;
    private Asset  asset;
    private String amount;

    public PaymentOperation (String sourceAccount,
                             String destinationAccount,
                             Asset asset,
                             String amount) {
        super(sourceAccount);
        this.destinationAccount = destinationAccount;
        this.asset = asset;
        this.amount = amount;
    }

    public String getDestinationAccount () {
        return destinationAccount;
    }

    public void setDestinationAccount (String destinationAccount) {
        this.destinationAccount = destinationAccount;
    }

    public Asset getAsset () {
        return asset;
    }

    public void setAsset (Asset asset) {
        this.asset = asset;
    }

    public String getAmount () {
        return amount;
    }

    public void setAmount (String amount) {
        this.amount = amount;
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PaymentOperation that = (PaymentOperation) o;
        return Objects.equals(destinationAccount, that.destinationAccount) &&
            Objects.equals(asset, that.asset) &&
            Objects.equals(amount, that.amount);
    }

    @Override
    public int hashCode () {
        return Objects.hash(destinationAccount, asset, amount);
    }
}
