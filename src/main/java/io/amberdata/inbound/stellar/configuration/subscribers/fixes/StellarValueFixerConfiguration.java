package io.amberdata.inbound.stellar.configuration.subscribers.fixes;

import io.amberdata.inbound.core.client.InboundApiClient;
import io.amberdata.inbound.domain.FunctionCall;
import io.amberdata.inbound.domain.Transaction;
import io.amberdata.inbound.stellar.client.HorizonServer;
import io.amberdata.inbound.stellar.configuration.properties.BatchSettings;
import io.amberdata.inbound.stellar.configuration.subscribers.StellarSubscriberConfiguration;
import io.amberdata.inbound.stellar.configuration.subscribers.SubscriberErrorsHandler;
import io.amberdata.inbound.stellar.mapper.AssetMapper;
import io.amberdata.inbound.stellar.mapper.ModelMapper;
import io.amberdata.inbound.stellar.mapper.operations.AccountMergeOperationMapper;
import io.amberdata.inbound.stellar.mapper.operations.CreateAccountOperationMapper;
import io.amberdata.inbound.stellar.mapper.operations.InflationOperationMapper;
import io.amberdata.inbound.stellar.mapper.operations.OperationMapper;
import io.amberdata.inbound.stellar.mapper.operations.PathPaymentStrictReceiveOperationMapper;
import io.amberdata.inbound.stellar.mapper.operations.PathPaymentStrictSendOperationMapper;
import io.amberdata.inbound.stellar.mapper.operations.PaymentOperationMapper;

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
  private final BatchSettings           batchSettings;
  private final SubscriberErrorsHandler errorsHandler;

  /**
   * Default constructor.
   *
   * @param apiClient         the client api
   * @param modelMapper       the model mapper
   * @param server            the Horizon server
   * @param batchSettings     the batch settings
   * @param errorsHandler     the error handler
   */
  public StellarValueFixerConfiguration (
      @Value("${stellar.fix.value.ledger.start}") Long startLedger,
      @Value("${stellar.fix.value.ledger.end}")   Long endLedger,
      InboundApiClient        apiClient,
      ModelMapper             modelMapper,
      HorizonServer           server,
      BatchSettings           batchSettings,
      SubscriberErrorsHandler errorsHandler
  ) {
    this.apiClient         = apiClient;
    this.modelMapper       = modelMapper;
    this.server            = server;
    this.batchSettings     = batchSettings;
    this.errorsHandler     = errorsHandler;
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

      for (TransactionResponse transaction : transactions) {
        List<OperationResponse> operations = StellarSubscriberConfiguration.getObjects(
            this.server,
            this.server.horizonServer()
                .operations()
                .forTransaction(transaction.getHash())
                .execute()
        );

        List<FunctionCall> functionCalls = new ArrayList<>();

        BigDecimal lumens = BigDecimal.ZERO;
        for (OperationResponse operation : operations) {
          OperationMapper mapper = null;

          if (operation instanceof AccountMergeOperationResponse ) {
            mapper = new AccountMergeOperationMapper(this.server);
          } else if (operation instanceof CreateAccountOperationResponse ) {
            mapper = new CreateAccountOperationMapper();
          } else if (operation instanceof InflationOperationResponse ) {
            mapper = new InflationOperationMapper();
          } else if (operation instanceof PathPaymentStrictReceiveOperationResponse ) {
            mapper = new PathPaymentStrictReceiveOperationMapper(assetMapper);
          } else if (operation instanceof PathPaymentStrictSendOperationResponse ) {
            mapper = new PathPaymentStrictSendOperationMapper(assetMapper);
          } else if (operation instanceof PaymentOperationResponse ) {
            mapper = new PaymentOperationMapper(assetMapper);
          }

          if (mapper != null) {
            FunctionCall functionCall = mapper.map(operation);
            functionCalls.add(functionCall);
            lumens = lumens.add(functionCall.getLumensTransferred());
          }
        }

        if ( ! functionCalls.isEmpty() ) {
          long timePublishFunctions = System.currentTimeMillis();
          this.apiClient.publish("/functions", functionCalls);
          StellarSubscriberConfiguration.logPerformance(
              "publishFunctions",
              functionCalls,
              timePublishFunctions
          );

          Transaction tx = this.modelMapper.mapTransaction(transaction, operations);
          long timePublishTransactions = System.currentTimeMillis();
          this.apiClient.publish("/transactions", tx);
          StellarSubscriberConfiguration.logPerformance(
              "publishTransaction",
              transactions,
              timePublishTransactions
          );
        }
      }
    }
  }
}
