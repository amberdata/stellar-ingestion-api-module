package io.amberdata.domain.operations;

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
}
