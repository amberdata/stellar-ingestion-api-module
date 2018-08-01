package io.amberdata.domain.operations;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CreateAccountOperation extends Operation {

    private String destinationAccount;
    private String startingBalance;

    public CreateAccountOperation (String sourceAccount,
                                   String destinationAccount,
                                   String startingBalance) {
        super(sourceAccount);
        this.destinationAccount = destinationAccount;
        this.startingBalance = startingBalance;
    }

    public String getDestinationAccount () {
        return destinationAccount;
    }

    public void setDestinationAccount (String destinationAccount) {
        this.destinationAccount = destinationAccount;
    }

    public String getStartingBalance () {
        return startingBalance;
    }

    public void setStartingBalance (String startingBalance) {
        this.startingBalance = startingBalance;
    }

    @Override
    public List<String> getInvolvedAccounts () {
        return Arrays.asList(getSourceAccount(), this.destinationAccount);
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CreateAccountOperation that = (CreateAccountOperation) o;
        return Objects.equals(destinationAccount, that.destinationAccount) &&
            Objects.equals(startingBalance, that.startingBalance);
    }

    @Override
    public int hashCode () {
        return Objects.hash(destinationAccount, startingBalance);
    }

    @Override
    public String toString () {
        return "CreateAccountOperation{" +
            "destinationAccount='" + destinationAccount + '\'' +
            ", startingBalance='" + startingBalance + '\'' +
            '}';
    }
}
