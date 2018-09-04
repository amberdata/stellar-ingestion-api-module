package io.amberdata.inbound.stellar.configuration.subscribers;

import java.util.Objects;

import org.stellar.sdk.responses.TradeResponse;

public class ExtendedTradeResponse {

    private TradeResponse tradeResponse;
    private Long ledger;
    private String transactionHash;
    private String operationHash;

    private ExtendedTradeResponse (TradeResponse tradeResponse,
                                   Long ledger,
                                   String transactionHash,
                                   String operationHash) {
        this.tradeResponse = tradeResponse;
        this.ledger = ledger;
        this.transactionHash = transactionHash;
        this.operationHash = operationHash;
    }

    public static ExtendedTradeResponse from (TradeResponse tradeResponse,
                                              Long ledger,
                                              String transactionHash,
                                              String operationHash) {
        return new ExtendedTradeResponse(tradeResponse, ledger, transactionHash, operationHash);
    }

    public TradeResponse getTradeResponse () {
        return tradeResponse;
    }

    public Long getLedger () {
        return ledger;
    }

    public String getTransactionHash () {
        return transactionHash;
    }

    public String getOperationHash () {
        return operationHash;
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ExtendedTradeResponse that = (ExtendedTradeResponse) o;
        return Objects.equals(tradeResponse, that.tradeResponse) &&
            Objects.equals(ledger, that.ledger) &&
            Objects.equals(transactionHash, that.transactionHash) &&
            Objects.equals(operationHash, that.operationHash);
    }

    @Override
    public int hashCode () {
        return Objects.hash(tradeResponse, ledger, transactionHash, operationHash);
    }

    @Override
    public String toString () {
        return "ExtendedTradeResponse{" +
            "tradeResponse=" + tradeResponse +
            ", ledger=" + ledger +
            ", transactionHash='" + transactionHash + '\'' +
            ", operationHash='" + operationHash + '\'' +
            '}';
    }
}
