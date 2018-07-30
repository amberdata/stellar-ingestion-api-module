package io.amberdata.domain.operations;

import java.util.Arrays;
import java.util.Objects;

public class ManageDataOperation extends Operation {

    private String name;
    private byte[] value;

    public ManageDataOperation (String sourceAccount,
                                String name,
                                byte[] value) {
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

    public byte[] getValue () {
        return value;
    }

    public void setValue (byte[] value) {
        this.value = value;
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
            Arrays.equals(value, that.value);
    }

    @Override
    public int hashCode () {
        int result = Objects.hash(name);
        result = 31 * result + Arrays.hashCode(value);
        return result;
    }

    @Override
    public String toString () {
        return "ManageDataOperation{" +
            "name='" + name + '\'' +
            ", value=" + Arrays.toString(value) +
            '}';
    }
}
