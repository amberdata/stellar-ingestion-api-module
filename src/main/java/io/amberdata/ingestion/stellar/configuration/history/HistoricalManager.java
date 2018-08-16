package io.amberdata.ingestion.stellar.configuration.history;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.stellar.sdk.Server;
import org.stellar.sdk.requests.RequestBuilder;
import org.stellar.sdk.responses.Page;
import org.stellar.sdk.responses.TransactionResponse;

import io.amberdata.ingestion.stellar.client.HorizonServer;

@Component
public class HistoricalManager {

    private static final Logger LOG = LoggerFactory.getLogger(HistoricalManager.class);

    private final boolean isActive;
    private final Server  horizonServer;

    private Long ledgerSequenceNumber;

    public HistoricalManager (
        @Value("${stellar.state.start-all-from-ledger}") Long ledgerSequenceNumber,
        HorizonServer horizonServer) {

        if (ledgerSequenceNumber != null && ledgerSequenceNumber > 0) {
            isActive = true;
            this.ledgerSequenceNumber = ledgerSequenceNumber;
        } else {
            isActive = false;
        }

        this.horizonServer = horizonServer.horizonServer();

    }

    public boolean disabled () {
        return !isActive;
    }

    public String ledgerPagingToken () {
        ensureIsActive();

        LOG.info("Going to request paging token for ledger with sequence number {}", ledgerSequenceNumber);

        try {
            return horizonServer.ledgers()
                .ledger(ledgerSequenceNumber)
                .getPagingToken();
        }
        catch (IOException e) {
            throw new IllegalStateException("Error occurred, provided ledger sequence: " + ledgerSequenceNumber);
        }
    }

    public String transactionPagingToken () {
        ensureIsActive();

        LOG.info(
            "Going to request paging token for first transaction in ledger with sequence number {}",
            ledgerSequenceNumber
        );

        try {
            Page<TransactionResponse> transactionsPage = horizonServer.transactions()
                .forLedger(ledgerSequenceNumber)
                .order(RequestBuilder.Order.ASC)
                .execute();

            ArrayList<TransactionResponse> transactions = transactionsPage.getRecords();
            if (transactions.isEmpty()) {
                LOG.info("There are no transactions in ledger with sequence number {} " +
                    "\n going to check the next ledger in sequence", ledgerSequenceNumber);

                return incrementAndGetNext();
            }
            return transactions.get(0).getPagingToken();
        }
        catch (IOException e) {
            throw new IllegalStateException("Error occurred, provided ledger sequence number: " + ledgerSequenceNumber);
        }
    }

    private String incrementAndGetNext () {
        long sleepTime = 100L;
        try {
            LOG.info("Sleeping for {}ms before request the next ledger in seqence", sleepTime);
            TimeUnit.MILLISECONDS.sleep(sleepTime);
        }
        catch (InterruptedException e) {
            LOG.error("Interrupted", e);
        }
        ledgerSequenceNumber++;

        return transactionPagingToken();
    }

    private void ensureIsActive () {
        if (disabled()) {
            throw new IllegalStateException("Historical Manager is not active. To enable it define ledger sequence number on startup");
        }
    }
}
