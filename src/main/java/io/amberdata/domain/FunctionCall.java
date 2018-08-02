package io.amberdata.domain;

import java.util.List;
import java.util.Objects;

public class FunctionCall {

    private String             name;
    private String             signature;
    private List<Argument>     arguments;
    private String             type;
    private String             from;
    private String             to;
    private String             assetType;
    private String             value;
    private String             meta;
    private List<FunctionCall> subFunctions;

    private FunctionCall (Builder builder) {
        this.name = builder.name;
        this.signature = builder.signature;
        this.arguments = builder.arguments;
        this.type = builder.type;
        this.from = builder.from;
        this.to = builder.to;
        this.assetType = builder.assetType;
        this.value = builder.value;
        this.meta = builder.meta;
        this.subFunctions = builder.subFunctions;
    }

    public String getName () {
        return name;
    }

    public void setName (String name) {
        this.name = name;
    }

    public String getSignature () {
        return signature;
    }

    public void setSignature (String signature) {
        this.signature = signature;
    }

    public List<Argument> getArguments () {
        return arguments;
    }

    public void setArguments (List<Argument> arguments) {
        this.arguments = arguments;
    }

    public String getType () {
        return type;
    }

    public void setType (String type) {
        this.type = type;
    }

    public String getFrom () {
        return from;
    }

    public void setFrom (String from) {
        this.from = from;
    }

    public String getTo () {
        return to;
    }

    public void setTo (String to) {
        this.to = to;
    }

    public String getAssetType () {
        return assetType;
    }

    public void setAssetType (String assetType) {
        this.assetType = assetType;
    }

    public String getValue () {
        return value;
    }

    public void setValue (String value) {
        this.value = value;
    }

    public String getMeta () {
        return meta;
    }

    public void setMeta (String meta) {
        this.meta = meta;
    }

    public List<FunctionCall> getSubFunctions () {
        return subFunctions;
    }

    public void setSubFunctions (List<FunctionCall> subFunctions) {
        this.subFunctions = subFunctions;
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FunctionCall that = (FunctionCall) o;
        return Objects.equals(name, that.name) &&
            Objects.equals(signature, that.signature) &&
            Objects.equals(arguments, that.arguments) &&
            Objects.equals(type, that.type) &&
            Objects.equals(from, that.from) &&
            Objects.equals(to, that.to) &&
            Objects.equals(assetType, that.assetType) &&
            Objects.equals(value, that.value) &&
            Objects.equals(meta, that.meta) &&
            Objects.equals(subFunctions, that.subFunctions);
    }

    @Override
    public int hashCode () {
        return Objects.hash(name, signature, arguments, type, from, to, assetType, value, meta, subFunctions);
    }

    public static class Builder {
        private String             name;
        private String             signature;
        private List<Argument>     arguments;
        private String             type;
        private String             from;
        private String             to;
        private String             assetType;
        private String             value;
        private String             meta;
        private List<FunctionCall> subFunctions;

        public FunctionCall.Builder name (String name) {
            this.name = name;
            return this;
        }

        public FunctionCall.Builder signature (String signature) {
            this.signature = signature;
            return this;
        }

        public FunctionCall.Builder arguments (List<Argument> arguments) {
            this.arguments = arguments;
            return this;
        }

        public FunctionCall.Builder type (String type) {
            this.type = type;
            return this;
        }

        public FunctionCall.Builder from (String from) {
            this.from = from;
            return this;
        }

        public FunctionCall.Builder to (String to) {
            this.to = to;
            return this;
        }

        public FunctionCall.Builder assetType (String assetType) {
            this.assetType = assetType;
            return this;
        }

        public FunctionCall.Builder value (String value) {
            this.value = value;
            return this;
        }

        public FunctionCall.Builder meta (String meta) {
            this.meta = meta;
            return this;
        }

        public FunctionCall.Builder subFunctions (List<FunctionCall> subFunctions) {
            this.subFunctions = subFunctions;
            return this;
        }

        public FunctionCall build () {
            return new FunctionCall(this);
        }
    }

    public static class Argument {
        private String name;
        private String value;

        private Argument (String name, String value) {
            this.name = name;
            this.value = value;
        }

        public static Argument from (String name, String value) {
            return new Argument(name, value);
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
    }
}
