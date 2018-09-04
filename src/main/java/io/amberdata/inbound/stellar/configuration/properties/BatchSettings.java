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

    public String getBlocksInChunk () {
        return this.blocksInChunk;
    }

    public void setBlocksInChunk (String blocksInChunk) {
        this.blocksInChunk = blocksInChunk;
    }

    public String getTransactionsInChunk () {
        return this.transactionsInChunk;
    }

    public void setTransactionsInChunk (String transactionsInChunk) {
        this.transactionsInChunk = transactionsInChunk;
    }

    public String getAddressesInChunk () {
        return addressesInChunk;
    }

    public void setAddressesInChunk (String addressesInChunk) {
        this.addressesInChunk = addressesInChunk;
    }

    public String getAssetsInChunk () {
        return assetsInChunk;
    }

    public void setAssetsInChunk (String assetsInChunk) {
        this.assetsInChunk = assetsInChunk;
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

    @Override
    public String toString () {
        return "BatchSettings{" +
            "blocksInChunk='" + this.blocksInChunk + '\'' +
            ", transactionsInChunk='" + this.transactionsInChunk + '\'' +
            ", addressesInChunk='" + this.addressesInChunk + '\'' +
            ", assetsInChunk='" + this.assetsInChunk + '\'' +
            '}';
    }
}
