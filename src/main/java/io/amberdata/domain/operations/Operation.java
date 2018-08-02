package io.amberdata.domain.operations;

import java.util.List;

import io.amberdata.domain.Asset;

public abstract class Operation {

    private String sourceAccount;

    public Operation (String sourceAccount) {
        this.sourceAccount = sourceAccount;
    }

    public String getSourceAccount () {
        return sourceAccount;
    }

    public void setSourceAccount (String sourceAccount) {
        this.sourceAccount = sourceAccount;
    }

    public abstract List<String> getInvolvedAccounts ();

    public abstract List<Asset> getInvolvedAssets ();
}
