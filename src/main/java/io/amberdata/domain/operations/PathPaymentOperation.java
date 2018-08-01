package io.amberdata.domain.operations;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.amberdata.domain.Asset;

public class PathPaymentOperation extends Operation {

    private Asset   sendAsset;
    private String  sendMax;
    private String  destinationAccount;
    private Asset   destinationAsset;
    private String  destinationAmount;
    private Asset[] path;

    public PathPaymentOperation (String sourceAccount,
                                 Asset sendAsset,
                                 String sendMax,
                                 String destinationAccount,
                                 Asset destinationAsset,
                                 String destinationAmount,
                                 Asset[] path) {
        super(sourceAccount);
        this.sendAsset = sendAsset;
        this.sendMax = sendMax;
        this.destinationAccount = destinationAccount;
        this.destinationAsset = destinationAsset;
        this.destinationAmount = destinationAmount;
        this.path = path;
    }

    public Asset getSendAsset () {
        return sendAsset;
    }

    public void setSendAsset (Asset sendAsset) {
        this.sendAsset = sendAsset;
    }

    public String getSendMax () {
        return sendMax;
    }

    public void setSendMax (String sendMax) {
        this.sendMax = sendMax;
    }

    public String getDestinationAccount () {
        return destinationAccount;
    }

    public void setDestinationAccount (String destinationAccount) {
        this.destinationAccount = destinationAccount;
    }

    public Asset getDestinationAsset () {
        return destinationAsset;
    }

    public void setDestinationAsset (Asset destinationAsset) {
        this.destinationAsset = destinationAsset;
    }

    public String getDestinationAmount () {
        return destinationAmount;
    }

    public void setDestinationAmount (String destinationAmount) {
        this.destinationAmount = destinationAmount;
    }

    public Asset[] getPath () {
        return path;
    }

    public void setPath (Asset[] path) {
        this.path = path;
    }

    @Override
    public List<String> getInvolvedAccounts () {
        return Arrays.asList(getSourceAccount(), this.destinationAccount);
    }

    @Override
    public List<Asset> getInvolvedAssets () {
        return Arrays.asList(sendAsset, destinationAsset);
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PathPaymentOperation that = (PathPaymentOperation) o;
        return Objects.equals(sendAsset, that.sendAsset) &&
            Objects.equals(sendMax, that.sendMax) &&
            Objects.equals(destinationAccount, that.destinationAccount) &&
            Objects.equals(destinationAsset, that.destinationAsset) &&
            Objects.equals(destinationAmount, that.destinationAmount) &&
            Arrays.equals(path, that.path);
    }

    @Override
    public int hashCode () {
        int result = Objects.hash(sendAsset, sendMax, destinationAccount, destinationAsset, destinationAmount);
        result = 31 * result + Arrays.hashCode(path);
        return result;
    }

    @Override
    public String toString () {
        return "PathPaymentOperation{" +
            "sendAsset=" + sendAsset +
            ", sendMax='" + sendMax + '\'' +
            ", destinationAccount='" + destinationAccount + '\'' +
            ", destinationAsset=" + destinationAsset +
            ", destinationAmount='" + destinationAmount + '\'' +
            ", path=" + Arrays.toString(path) +
            '}';
    }
}
