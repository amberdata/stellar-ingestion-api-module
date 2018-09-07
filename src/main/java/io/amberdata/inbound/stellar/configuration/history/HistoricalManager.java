package io.amberdata.inbound.stellar.configuration.history;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.amberdata.inbound.core.configuration.InboundApiProperties;
import io.amberdata.inbound.stellar.client.HorizonServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import org.stellar.sdk.Server;
import org.stellar.sdk.requests.RequestBuilder;
import org.stellar.sdk.requests.TooManyRequestsException;
import org.stellar.sdk.responses.Page;
import org.stellar.sdk.responses.TransactionResponse;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class HistoricalManager {

  private static final Logger LOG = LoggerFactory.getLogger(HistoricalManager.class);

  private final boolean isActive;
  private final Server horizonServer;
  private final Long ledgerSequenceNumber;

  private String lastLedgerToken;
  private String lastTransactionToken;

  public HistoricalManager(
      @Value("${stellar.state.start-all-from-ledger}") Long ledgerSequenceNumber,
      HorizonServer horizonServer,
      InboundApiProperties apiProperties
  ) {
    if (ledgerSequenceNumber != null && ledgerSequenceNumber > 0) {
      this.isActive = true;
      this.ledgerSequenceNumber = ledgerSequenceNumber;
    } else {
      this.isActive = false;
      this.ledgerSequenceNumber = this.getLedgerSequenceNumber(apiProperties);
    }

    this.horizonServer = horizonServer.horizonServer();
  }

  public boolean disabled() {
    return !this.isActive;
  }

  public synchronized String ledgerPagingToken() {
    ensureIsActive();

    if (this.lastLedgerToken != null) {
      return this.lastLedgerToken;
    }

    LOG.info(
        "Going to request paging token for ledger with sequence number {}",
        this.ledgerSequenceNumber
    );

    try {
      String token = this.horizonServer.ledgers()
          .ledger(this.ledgerSequenceNumber)
          .getPagingToken();

      this.lastLedgerToken = token;

      return token;
    } catch (Exception ex) {
      throw new IllegalStateException(
          "Error occurred, provided ledger sequence: " + this.ledgerSequenceNumber,
          ex
      );
    }
  }

  public synchronized String transactionPagingToken() {
    this.ensureIsActive();

    if (this.lastTransactionToken != null) {
      return this.lastTransactionToken;
    }

    String token = transactionPagingToken(this.ledgerSequenceNumber);
    this.lastTransactionToken = token;

    return token;
  }

  private String transactionPagingToken(Long seqNumber) {
    LOG.info(
        "Going to request paging token for first transaction in ledger with sequence number {}",
        seqNumber
    );

    try {
      List<TransactionResponse> transactions = Collections.emptyList();

      while (transactions.isEmpty()) {
        Page<TransactionResponse> transactionsPage = this.horizonServer.transactions()
            .forLedger(seqNumber)
            .order(RequestBuilder.Order.ASC)
            .execute();

        transactions = transactionsPage.getRecords();

        if (transactions.isEmpty()) {
          LOG.info(
              "There are no transactions in ledger with sequence number {} \n"
              + " going to check the next ledger in sequence", seqNumber
          );
          waitBeforeNextLedgerProcessing();
        }

        ++seqNumber;
      }
      return transactions.get(0).getPagingToken();
    } catch (TooManyRequestsException tmre) {
      throw tmre;
    } catch (Exception ex) {
      // this kind of exception will make the app to stop with fatal error
      throw new IllegalStateException(
          "Error occurred, provided ledger sequence number: " + this.ledgerSequenceNumber,
          ex
      );
    }
  }

  private void defaultHttpHeaders(InboundApiProperties apiProperties, HttpHeaders httpHeaders) {
    httpHeaders.add("x-amberdata-blockchain-id", apiProperties.getBlockchainId());
    httpHeaders.add("x-amberdata-api-key", apiProperties.getApiKey());
    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
  }

  private Long getLedgerSequenceNumber(InboundApiProperties apiProperties) {
    WebClient webClient = WebClient.builder()
        .baseUrl(apiProperties.getUrl())
        .defaultHeaders(httpHeaders -> this.defaultHttpHeaders(apiProperties, httpHeaders))
        .build();

    String response = webClient.get()
        .uri("/blocks/last")
        .retrieve()
        .bodyToMono(String.class)
        .block();

    try {
      @SuppressWarnings({"rawtypes", "unchecked"})
      Map<String, Object> payload = (Map<String, Object>) new ObjectMapper().readValue(
          response,
          Map.class
      ).get("payload");

      return payload == null
          ? Long.valueOf(0L)
          : Long.parseLong((String) payload.get("number"));
    } catch (Exception ex) {
      return Long.valueOf(0L);
    }
  }

  private void waitBeforeNextLedgerProcessing() {
    long sleepTime = 100L;
    try {
      LOG.info("Sleeping for {}ms before request the next ledger in sequence", sleepTime);
      TimeUnit.MILLISECONDS.sleep(sleepTime);
    } catch (InterruptedException ie) {
      LOG.error("Interrupted", ie);
    }
  }

  private void ensureIsActive() {
    if (this.disabled()) {
      throw new IllegalStateException(
          "Historical Manager is not active. To enable it define ledger sequence number on startup"
      );
    }
  }
}
