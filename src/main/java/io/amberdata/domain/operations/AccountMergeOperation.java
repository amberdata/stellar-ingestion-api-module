package io.amberdata.domain.operations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.amberdata.domain.Asset;

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
    public List<String> getInvolvedAccounts () {
        return Arrays.asList(getSourceAccount(), this.destination);
    }

    @Override
    public List<Asset> getInvolvedAssets () {
        return Collections.emptyList();
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
