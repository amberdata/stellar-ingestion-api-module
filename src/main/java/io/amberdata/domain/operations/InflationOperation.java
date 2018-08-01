package io.amberdata.domain.operations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class InflationOperation extends Operation {

    public InflationOperation (String sourceAccount) {
        super(sourceAccount);
    }

    @Override
    public List<String> getInvolvedAccounts () {
        return Collections.singletonList(getSourceAccount());
    }

    @Override
    public String toString () {
        return "InflationOperation{}";
    }
}
