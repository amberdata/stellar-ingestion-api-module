package io.amberdata.domain;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Transaction implements BlockchainEntity {
    private String              blockchainId;
    private String              hash;
    private BigInteger          nonce;
    private BigInteger          blockNumber;
    private String              from;
    private BigInteger          gas;
    private BigInteger          gasUsed;
    private Integer             numLogs;
    private Long                timestamp;
    private List<FunctionCall>  functionCalls;
    private Map<String, Object> optionalProperties;

    public Transaction (Builder builder) {
        this.blockchainId       = builder.blockchainId;
        this.hash               = builder.hash;
        this.nonce              = builder.nonce;
        this.blockNumber        = builder.blockNumber;
        this.from               = builder.from;
        this.gas                = builder.gas;
        this.gasUsed            = builder.gasUsed;
        this.numLogs            = builder.numLogs;
        this.timestamp          = builder.timestamp;
        this.functionCalls      = builder.functionCalls;
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

    public BigInteger getNonce () {
        return nonce;
    }

    public void setNonce (BigInteger nonce) {
        this.nonce = nonce;
    }

    public BigInteger getBlockNumber () {
        return blockNumber;
    }

    public void setBlockNumber (BigInteger blockNumber) {
        this.blockNumber = blockNumber;
    }

    public String getFrom () {
        return from;
    }

    public void setFrom (String from) {
        this.from = from;
    }

    public BigInteger getGas () {
        return gas;
    }

    public void setGas (BigInteger gas) {
        this.gas = gas;
    }

    public BigInteger getGasUsed () {
        return gasUsed;
    }

    public void setGasUsed (BigInteger gasUsed) {
        this.gasUsed = gasUsed;
    }

    public Integer getNumLogs () {
        return numLogs;
    }

    public void setNumLogs (Integer numLogs) {
        this.numLogs = numLogs;
    }

    public Long getTimestamp () {
        return timestamp;
    }

    public void setTimestamp (Long timestamp) {
        this.timestamp = timestamp;
    }

    public List<FunctionCall> getFunctionCalls () {
        return functionCalls;
    }

    public void setFunctionCalls (List<FunctionCall> functionCalls) {
        this.functionCalls = functionCalls;
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
        Transaction that = (Transaction) o;
        return Objects.equals(blockchainId, that.blockchainId) &&
            Objects.equals(hash, that.hash);
    }

    @Override
    public int hashCode () {
        return Objects.hash(blockchainId, hash);
    }

    @Override
    public String toString () {
        return "Transaction{" +
            "blockchainId='" + blockchainId + '\'' +
            ", hash='" + hash + '\'' +
            ", nonce=" + nonce +
            ", blockNumber=" + blockNumber +
            ", from='" + from + '\'' +
            ", gas=" + gas +
            ", gasUsed=" + gasUsed +
            ", numLogs=" + numLogs +
            ", timestamp=" + timestamp +
            ", optionalProperties=" + optionalProperties +
            '}';
    }

    public static class Builder {
        private String              blockchainId;
        private String              hash;
        private BigInteger          nonce;
        private BigInteger          blockNumber;
        private String              from;
        private BigInteger          gas;
        private BigInteger          gasUsed;
        private Integer             numLogs;
        private Long                timestamp;
        private List<FunctionCall>  functionCalls;
        private Map<String, Object> optionalProperties;

        public Transaction.Builder blockchainId (String value) {
            this.blockchainId = value;
            return this;
        }

        public Transaction.Builder hash (String value) {
            this.hash = value;
            return this;
        }

        public Transaction.Builder nonce (BigInteger value) {
            this.nonce = value;
            return this;
        }

        public Transaction.Builder blockNumber (BigInteger value) {
            this.blockNumber = value;
            return this;
        }

        public Transaction.Builder from (String value) {
            this.from = value;
            return this;
        }

        public Transaction.Builder gas (BigInteger value) {
            this.gas = value;
            return this;
        }

        public Transaction.Builder gasUsed (BigInteger value) {
            this.gasUsed = value;
            return this;
        }

        public Transaction.Builder numLogs (Integer value) {
            this.numLogs = value;
            return this;
        }

        public Transaction.Builder timestamp (Long value) {
            this.timestamp = value;
            return this;
        }

        public Transaction.Builder functionCalls (List<FunctionCall> functionCalls) {
            this.functionCalls = functionCalls;
            return this;
        }

        public Transaction.Builder optionalProperties (Map<String, Object> value) {
            this.optionalProperties = value;
            return this;
        }

        public Transaction build () {
            return new Transaction(this);
        }
    }
}
