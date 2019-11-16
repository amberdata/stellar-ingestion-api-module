package io.amberdata.inbound.stellar.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("inbound.api.batch")
public class BatchSettings {

  private String blocksInChunk;
  private String transactionsInChunk;
  private String addressesInChunk;
  private String assetsInChunk;
  private String ordersInChunk;
  private String tradesInChunk;

  public String getBlocksInChunk() {
    return this.blocksInChunk;
  }

  public void setBlocksInChunk(String blocksInChunk) {
    this.blocksInChunk = blocksInChunk;
  }

  public String getTransactionsInChunk() {
    return this.transactionsInChunk;
  }

  public void setTransactionsInChunk(String transactionsInChunk) {
    this.transactionsInChunk = transactionsInChunk;
  }

  public String getAddressesInChunk() {
    return this.addressesInChunk;
  }

  public void setAddressesInChunk(String addressesInChunk) {
    this.addressesInChunk = addressesInChunk;
  }

  public String getAssetsInChunk() {
    return this.assetsInChunk;
  }

  public void setAssetsInChunk(String assetsInChunk) {
    this.assetsInChunk = assetsInChunk;
  }

  public String getOrdersInChunk() {
    return this.ordersInChunk;
  }

  public void setOrdersInChunk(String ordersInChunk) {
    this.ordersInChunk = ordersInChunk;
  }

  public String getTradesInChunk() {
    return this.tradesInChunk;
  }

  public void setTradesInChunk(String tradesInChunk) {
    this.tradesInChunk = tradesInChunk;
  }

  public int blocksInChunk() {
    return Integer.parseInt(getBlocksInChunk());
  }

  public int transactionsInChunk() {
    return Integer.parseInt(getTransactionsInChunk());
  }

  public int addressesInChunk() {
    return Integer.parseInt(getAddressesInChunk());
  }

  public int assetsInChunk() {
    return Integer.parseInt(getAssetsInChunk());
  }

  public int ordersInChunk() {
    return Integer.parseInt(getOrdersInChunk());
  }

  public int tradesInChunk() {
    return Integer.parseInt(getTradesInChunk());
  }

  @Override
  public String toString() {
    return
        "BatchSettings{"
        + "blocksInChunk='" + blocksInChunk + '\''
        + ", transactionsInChunk='" + transactionsInChunk + '\''
        + ", addressesInChunk='" + addressesInChunk + '\''
        + ", assetsInChunk='" + assetsInChunk + '\''
        + ", ordersInChunk='" + ordersInChunk + '\''
        + ", tradesInChunk='" + tradesInChunk + '\''
        + '}';
  }

}
