package io.amberdata.domain.operations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.amberdata.domain.Asset;

public class CreatePassiveOfferOperation extends Operation {

    private Asset  selling;
    private Asset  buying;
    private String amount;
    private String price;

    public CreatePassiveOfferOperation (String sourceAccount,
                                        Asset selling,
                                        Asset buying,
                                        String amount, String price) {
        super(sourceAccount);
        this.selling = selling;
        this.buying = buying;
        this.amount = amount;
        this.price = price;
    }

    public Asset getSelling () {
        return selling;
    }

    public void setSelling (Asset selling) {
        this.selling = selling;
    }

    public Asset getBuying () {
        return buying;
    }

    public void setBuying (Asset buying) {
        this.buying = buying;
    }

    public String getAmount () {
        return amount;
    }

    public void setAmount (String amount) {
        this.amount = amount;
    }

    public String getPrice () {
        return price;
    }

    public void setPrice (String price) {
        this.price = price;
    }

    @Override
    public List<String> getInvolvedAccounts () {
        return Collections.singletonList(getSourceAccount());
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CreatePassiveOfferOperation that = (CreatePassiveOfferOperation) o;
        return Objects.equals(selling, that.selling) &&
            Objects.equals(buying, that.buying) &&
            Objects.equals(amount, that.amount) &&
            Objects.equals(price, that.price);
    }

    @Override
    public int hashCode () {
        return Objects.hash(selling, buying, amount, price);
    }

    @Override
    public String toString () {
        return "CreatePassiveOfferOperation{" +
            "selling=" + selling +
            ", buying=" + buying +
            ", amount='" + amount + '\'' +
            ", price='" + price + '\'' +
            '}';
    }
}
