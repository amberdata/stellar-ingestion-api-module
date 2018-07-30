package io.amberdata.domain.operations;

public class InflationOperation extends Operation {

    public InflationOperation (String sourceAccount) {
        super(sourceAccount);
    }

    @Override
    public String toString () {
        return "InflationOperation{}";
    }
}
