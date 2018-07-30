package io.amberdata.domain;

import java.util.Map;
import java.util.Objects;

public final class Address {
    private String              blockchainId;
    private String              hash;
    private Long                timestamp; // TODO get it somewhere from Stellar
    private Map<String, Object> optionalProperties;

    private Address (Builder builder) {
        this.blockchainId       = builder.blockchainId;
        this.hash               = builder.hash;
        this.timestamp          = builder.timestamp;
        this.optionalProperties = builder.optionalProperties;
    }

    public String getBlockchainId () {
        return blockchainId;
    }

    public void setBlockchainId (String blockchainId) {
        this.blockchainId = blockchainId;
    }

    public String getHash () {
        return hash;
    }

    public void setHash (String hash) {
        this.hash = hash;
    }

    public Long getTimestamp () {
        return timestamp;
    }

    public void setTimestamp (Long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getOptionalProperties () {
        return optionalProperties;
    }

    public void setOptionalProperties (Map<String, Object> optionalProperties) {
        this.optionalProperties = optionalProperties;
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Address address = (Address) o;
        return Objects.equals(blockchainId, address.blockchainId) &&
            Objects.equals(hash, address.hash) &&
            Objects.equals(timestamp, address.timestamp) &&
            Objects.equals(optionalProperties, address.optionalProperties);
    }

    @Override
    public int hashCode () {
        return Objects.hash(blockchainId, hash, timestamp, optionalProperties);
    }

    @Override
    public String toString () {
        return "Address{" +
            "blockchainId='" + blockchainId + '\'' +
            ", hash='" + hash + '\'' +
            ", timestamp=" + timestamp +
            ", optionalProperties=" + optionalProperties +
            '}';
    }

    public static class Builder {
        private String              blockchainId;
        private String              hash;
        private Long                timestamp;
        private Map<String, Object> optionalProperties;

        public Address.Builder blockchainId (String value) {
            this.blockchainId = value;
            return this;
        }

        public Address.Builder hash (String value) {
            this.hash = value;
            return this;
        }

        public Address.Builder timestamp (Long value) {
            this.timestamp = value;
            return this;
        }

        public Address.Builder optionalProperties(Map<String, Object> value) {
            this.optionalProperties = value;
            return this;
        }

        public Address build () {
            return new Address(this);
        }
    }
}
