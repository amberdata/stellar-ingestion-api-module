package io.amberdata.inbound.stellar.configuration.subscribers.fixes;

import io.amberdata.inbound.core.client.InboundApiClient;
import io.amberdata.inbound.domain.FunctionCall;
import io.amberdata.inbound.stellar.client.HorizonServer;
import io.amberdata.inbound.stellar.configuration.subscribers.StellarSubscriberConfiguration;
import io.amberdata.inbound.stellar.mapper.AssetMapper;
import io.amberdata.inbound.stellar.mapper.ModelMapper;

import java.io.IOException;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import org.stellar.sdk.responses.TransactionResponse;
import org.stellar.sdk.responses.operations.AccountMergeOperationResponse;
import org.stellar.sdk.responses.operations.CreateAccountOperationResponse;
import org.stellar.sdk.responses.operations.InflationOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PathPaymentStrictReceiveOperationResponse;
import org.stellar.sdk.responses.operations.PathPaymentStrictSendOperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;

@Configuration
@ConditionalOnProperty(prefix = "stellar", name = "subscribe-on-fix-value")
public class StellarValueFixerConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(StellarValueFixerConfiguration.class);

  private final InboundApiClient        apiClient;
  private final ModelMapper             modelMapper;
  private final HorizonServer           server;

  /**
   * Default constructor.
   *
   * @param apiClient   the client api
   * @param modelMapper the model mapper
   * @param server      the Horizon server
   */
  public StellarValueFixerConfiguration (
      @Value("${stellar.fix.value.ledger.start}") Long startLedger,
      @Value("${stellar.fix.value.ledger.end}")   Long endLedger,
      InboundApiClient apiClient,
      ModelMapper      modelMapper,
      HorizonServer    server
  ) {
    this.apiClient   = apiClient;
    this.modelMapper = modelMapper;
    this.server      = server;
  }

  /**
   * Creates the global pipeline.
   */
  @PostConstruct
  public void createPipeline() throws IOException {
    LOG.info("Going to subscribe on Stellar Ledgers stream");

    AssetMapper assetMapper = new AssetMapper();

    for (long number=26261000; number<26261001; ++number) {
      List<TransactionResponse> transactions = StellarSubscriberConfiguration.getObjects(
          this.server,
          this.server.horizonServer()
              .transactions()
              .forLedger(number)
              .execute()
      );
      System.out.println("Ledger: " + number + ", # transactions: " + transactions.size());

      for (TransactionResponse transaction : transactions) {
        List<OperationResponse> operationResponses = StellarSubscriberConfiguration.getObjects(
            this.server,
            this.server.horizonServer()
                .operations()
                .forTransaction(transaction.getHash())
                .execute()
        );
        System.out.println("Ledger: " + number + ", transaction: " + transaction.getHash() + ", # operations: " + operationResponses.size());

        List<OperationResponse> operations = new ArrayList<>();
        for (OperationResponse operationResponse : operationResponses) {
          if (
              operationResponse instanceof AccountMergeOperationResponse
              || operationResponse instanceof CreateAccountOperationResponse
              || operationResponse instanceof InflationOperationResponse
              || operationResponse instanceof PathPaymentStrictReceiveOperationResponse
              || operationResponse instanceof PathPaymentStrictSendOperationResponse
              || operationResponse instanceof PaymentOperationResponse
          ) {
            operations.add(operationResponse);
          }
        }
        System.out.println("Ledger: " + number + ", transaction: " + transaction.getHash() + ", # operations: " + operations.size());

        if ( ! operations.isEmpty() ) {
          List<FunctionCall> functionCalls = this.modelMapper.mapOperations(operations, Long.valueOf(number));
          BigDecimal         lumens        = BigDecimal.ZERO;
          for (FunctionCall functionCall : functionCalls) {
            lumens = lumens.add(functionCall.getLumensTransferred());
          }

          long timePublishFunctions = System.currentTimeMillis();
          this.apiClient.publish("/functions", functionCalls);
          StellarSubscriberConfiguration.logPerformance(
              "publishFunctions",
              functionCalls,
              timePublishFunctions
          );

          System.out.println(transaction.getHash() + " -- " + lumens);
//          Transaction tx = this.modelMapper.mapTransaction(transaction, operations);
//          long timePublishTransactions = System.currentTimeMillis();
//          this.apiClient.publish("/transactions", tx);
//          StellarSubscriberConfiguration.logPerformance(
//              "publishTransaction",
//              transactions,
//              timePublishTransactions
//          );
        }
      }
    }
  }
}
