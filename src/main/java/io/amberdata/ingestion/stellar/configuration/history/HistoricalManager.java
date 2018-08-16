package io.amberdata.ingestion.stellar.configuration.history;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.stellar.sdk.Server;
import org.stellar.sdk.responses.LedgerResponse;

import io.amberdata.ingestion.stellar.client.HorizonServer;

@Component
public class HistoricalManager {
    private final boolean isActive;
    private final Long    ledgerSequenceNumber;
    private final Server  horizonServer;

    public HistoricalManager (
        @Value("${stellar.state.start-all-from-ledger}") Long ledgerSequenceNumber,
        HorizonServer horizonServer) {

        if (ledgerSequenceNumber != null && ledgerSequenceNumber > 0) {
            isActive = true;
            this.ledgerSequenceNumber = ledgerSequenceNumber;
        } else {
            isActive = false;
            this.ledgerSequenceNumber = -1L;
        }

        this.horizonServer = horizonServer.horizonServer();

    }

    public boolean isActive () {
        return isActive;
    }

    public String ledgerPagingToken () {
        ensureIsActive();

        try {
            LedgerResponse ledgerResponse = horizonServer.ledgers().ledger(ledgerSequenceNumber);
            return ledgerResponse.getPagingToken();
        }
        catch (IOException e) {
            throw new IllegalStateException("Error occurred, provided ledger sequence: " + ledgerSequenceNumber);
        }
    }

    public String transactionPagingToken () {
        ensureIsActive();

        //horizonServer.transactions().forLedger(ledgerSequenceNumber).

        return "now";
    }

    private void ensureIsActive () {
        if (!isActive()) {
            throw new IllegalStateException("Historical Manager is not active. To enable it define ledger sequence number on startup");
        }
    }
}
