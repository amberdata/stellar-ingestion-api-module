package io.amberdata.domain.operations;

import java.util.Objects;

public class AccountMergeOperation extends Operation {

    private String destination;

    public AccountMergeOperation (String sourceAccount,
                                  String destination) {
        super(sourceAccount);
        this.destination = destination;
    }

    public String getDestination () {
        return destination;
    }

    public void setDestination (String destination) {
        this.destination = destination;
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AccountMergeOperation that = (AccountMergeOperation) o;
        return Objects.equals(destination, that.destination);
    }

    @Override
    public int hashCode () {
        return Objects.hash(destination);
    }

    @Override
    public String toString () {
        return "AccountMergeOperation{" +
            "destination='" + destination + '\'' +
            '}';
    }
}
