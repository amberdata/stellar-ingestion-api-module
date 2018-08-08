package io.amberdata.domain;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Transaction implements BlockchainEntity {
    private String              hash;
    private BigInteger          nonce;
    private BigInteger          blockNumber;
    private String              from;
    private BigInteger          gas;
    private BigInteger          gasUsed;
    private Integer             numLogs;
    private Long                timestamp;
    private List<FunctionCall>  functionCalls;
    private String              status;
    private BigDecimal          value;
    private Map<String, Object> optionalProperties;

    public Transaction (Builder builder) {
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

    public String getStatus () {
        return status;
    }

    public void setStatus (String status) {
        this.status = status;
    }

    public BigDecimal getValue () {
        return value;
    }

    public void setValue (BigDecimal value) {
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
        Transaction that = (Transaction) o;
        return Objects.equals(hash, that.hash);
    }

    @Override
    public int hashCode () {
        return Objects.hash(hash);
    }

    @Override
    public String toString () {
        return "Transaction{" +
            "hash='" + hash + '\'' +
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
        private String              hash;
        private BigInteger          nonce;
        private BigInteger          blockNumber;
        private String              from;
        private BigInteger          gas;
        private BigInteger          gasUsed;
        private Integer             numLogs;
        private Long                timestamp;
        private List<FunctionCall>  functionCalls;
        private String              status;
        private BigDecimal          value;
        private Map<String, Object> optionalProperties;

        public Transaction.Builder hash (String aValue) {
            this.hash = aValue;
            return this;
        }

        public Transaction.Builder nonce (BigInteger aValue) {
            this.nonce = aValue;
            return this;
        }

        public Transaction.Builder blockNumber (BigInteger aValue) {
            this.blockNumber = aValue;
            return this;
        }

        public Transaction.Builder from (String aValue) {
            this.from = aValue;
            return this;
        }

        public Transaction.Builder gas (BigInteger aValue) {
            this.gas = aValue;
            return this;
        }

        public Transaction.Builder gasUsed (BigInteger aValue) {
            this.gasUsed = aValue;
            return this;
        }

        public Transaction.Builder numLogs (Integer aValue) {
            this.numLogs = aValue;
            return this;
        }

        public Transaction.Builder timestamp (Long aValue) {
            this.timestamp = aValue;
            return this;
        }

        public Transaction.Builder functionCalls (List<FunctionCall> aValue) {
            this.functionCalls = aValue;
            return this;
        }

        public Transaction.Builder status (String aValue) {
            this.status = aValue;
            return this;
        }

        public Transaction.Builder value (BigDecimal aValue) {
            this.value = aValue;
            return this;
        }

        public Transaction.Builder optionalProperties (Map<String, Object> aValue) {
            this.optionalProperties = aValue;
            return this;
        }

        public Transaction build () {
            return new Transaction(this);
        }
    }
}
