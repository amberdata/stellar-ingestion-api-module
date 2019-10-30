package io.amberdata.inbound.stellar.configuration.subscribers;

import java.util.Objects;

import org.stellar.sdk.responses.TradeResponse;

public class ExtendedTradeResponse {
  private TradeResponse tradeResponse;
  private Long          ledger;
  private String        transactionHash;
  private String        operationHash;

  private ExtendedTradeResponse(
      TradeResponse tradeResponse,
      Long          ledger,
      String        transactionHash,
      String        operationHash
  ) {
    this.tradeResponse   = tradeResponse;
    this.ledger          = ledger;
    this.transactionHash = transactionHash;
    this.operationHash   = operationHash;
  }

  public static ExtendedTradeResponse from(
      TradeResponse tradeResponse,
      Long          ledger,
      String        transactionHash,
      String        operationHash
  ) {
    return new ExtendedTradeResponse(tradeResponse, ledger, transactionHash, operationHash);
  }

  public TradeResponse getTradeResponse() {
    return this.tradeResponse;
  }

  public Long getLedger() {
    return this.ledger;
  }

  public String getTransactionHash() {
    return this.transactionHash;
  }

  public String getOperationHash() {
    return this.operationHash;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }

    if (object == null || this.getClass() != object.getClass()) {
      return false;
    }

    ExtendedTradeResponse that = (ExtendedTradeResponse) object;
    return
        Objects.equals(this.tradeResponse, that.tradeResponse)
            && Objects.equals(this.ledger, that.ledger)
            && Objects.equals(this.transactionHash, that.transactionHash)
            && Objects.equals(this.operationHash, that.operationHash);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.tradeResponse, this.ledger, this.transactionHash, this.operationHash);
  }

  @Override
  public String toString() {
    return
        "ExtendedTradeResponse{"
        + "tradeResponse=" + this.tradeResponse
        + ", ledger=" + this.ledger
        + ", transactionHash='" + this.transactionHash + '\''
        + ", operationHash='" + this.operationHash + '\''
        + '}';
  }

}
