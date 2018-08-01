package io.amberdata.domain.operations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ManageDataOperation extends Operation {

    private String name;
    private String value;

    public ManageDataOperation (String sourceAccount,
                                String name,
                                String value) {
        super(sourceAccount);
        this.name = name;
        this.value = value;
    }

    public String getName () {
        return name;
    }

    public void setName (String name) {
        this.name = name;
    }

    public String getValue () {
        return value;
    }

    public void setValue (String value) {
        this.value = value;
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
        ManageDataOperation that = (ManageDataOperation) o;
        return Objects.equals(name, that.name) &&
            Objects.equals(value, that.value);
    }

    @Override
    public int hashCode () {
        return Objects.hash(name, value);
    }

    @Override
    public String toString () {
        return "ManageDataOperation{" +
            "name='" + name + '\'' +
            ", value='" + value + '\'' +
            '}';
    }
}
