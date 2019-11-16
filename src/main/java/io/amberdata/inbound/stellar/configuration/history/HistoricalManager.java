package io.amberdata.inbound.stellar.configuration.history;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.amberdata.inbound.core.configuration.InboundApiProperties;
import io.amberdata.inbound.stellar.client.HorizonServer;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

@Component
public class HistoricalManager {

  private static final Logger LOG = LoggerFactory.getLogger(HistoricalManager.class);

  private final boolean isActive;
  private final Server  horizonServer;
  private final Long    ledgerSequenceNumberStart;
  private final Long    ledgerSequenceNumberEnd;

  private String lastLedgerToken;
  private String lastTransactionToken;

  /**
   * Default constructor.
   *
   * @param ledgerSequenceNumberStart the ledger number to start the historical manager from
   * @param ledgerSequenceNumberEnd   the ledger number to end   the historical manager on
   * @param horizonServer             the Horizon server
   * @param apiProperties             the API properties
   */
  public HistoricalManager(
      @Value("${stellar.state.start-all-from-ledger}") Long ledgerSequenceNumberStart,
      @Value("${stellar.state.end-all-from-ledger}")   Long ledgerSequenceNumberEnd,
      HorizonServer        horizonServer,
      InboundApiProperties apiProperties
  ) {
    if (ledgerSequenceNumberStart != null && ledgerSequenceNumberStart > 0) {
      this.isActive = true;
      this.ledgerSequenceNumberStart = ledgerSequenceNumberStart;
    } else {
      this.isActive = false;
      this.ledgerSequenceNumberStart = this.getLedgerSequenceNumber(apiProperties);
    }
    this.ledgerSequenceNumberEnd = ledgerSequenceNumberEnd;

    this.horizonServer = horizonServer.horizonServer();
  }

  /**
   * Returns true if the historical manager is not enabled.
   *
   * @return True if the historical manager is disabled.
   */
  public boolean disabled() {
    return !this.isActive;
  }

  public Long getLastLedger () {
    return this.ledgerSequenceNumberEnd;
  }

  /**
   * Returns the token to paginate ledgers.
   *
   * @return The token to paginate ledgers.
   */
  public synchronized String ledgerPagingToken() {
    this.ensureIsActive();

    if (this.lastLedgerToken != null) {
      return this.lastLedgerToken;
    }

    LOG.info(
        "Going to request paging token for ledger with sequence number {}",
        this.ledgerSequenceNumberStart
    );

    try {
      String token = this.horizonServer.ledgers()
          .ledger(this.ledgerSequenceNumberStart)
          .getPagingToken();

      this.lastLedgerToken = token;

      return token;
    } catch (Exception e) {
      throw new IllegalStateException(
          "Error occurred, provided ledger sequence: " + this.ledgerSequenceNumberStart,
          e
      );
    }
  }

  /**
   * Returns the token to paginate transactions.
   *
   * @return the token to paginate transactions.
   */
  public synchronized String transactionPagingToken() {
    this.ensureIsActive();

    if (this.lastTransactionToken != null) {
      return this.lastTransactionToken;
    }

    String token = this.transactionPagingToken(this.ledgerSequenceNumberStart);
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
          this.waitBeforeNextLedgerProcessing();
        }

        ++seqNumber;
      }
      return transactions.get(0).getPagingToken();
    } catch (TooManyRequestsException tmre) {
      throw tmre;
    } catch (Exception e) {
      // This kind of exception will make the app to stop with fatal error
      throw new IllegalStateException(
          "Error occurred, provided ledger sequence number: " + this.ledgerSequenceNumberStart,
          e
      );
    }
  }

  private void defaultHttpHeaders(InboundApiProperties apiProperties, HttpHeaders httpHeaders) {
    httpHeaders.add("x-amberdata-blockchain-id", apiProperties.getBlockchainId());
    httpHeaders.add("x-amberdata-api-key",       apiProperties.getApiKey());
    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
  }

  private Long getLedgerSequenceNumber(InboundApiProperties apiProperties) {
    try {
      WebClient webClient = WebClient.builder()
          .baseUrl(apiProperties.getUrl())
          .defaultHeaders(httpHeaders -> this.defaultHttpHeaders(apiProperties, httpHeaders))
          .build();

      String response = webClient.get()
          .uri("/blocks/last")
          .retrieve()
          .bodyToMono(String.class)
          .block();

      @SuppressWarnings({"rawtypes", "unchecked"})
      Map<String, Object> payload = (Map<String, Object>) new ObjectMapper().readValue(
          response,
          Map.class
      ).get("payload");

      return payload == null
          ? Long.valueOf(0L)
          : Long.parseLong((String) payload.get("number"));
    } catch (Exception e) {
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
