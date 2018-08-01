package io.amberdata.domain.operations;

import java.util.List;

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
}
