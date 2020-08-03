package io.amberdata.inbound.stellar.mapper.operations;

import io.amberdata.inbound.domain.Asset;
import io.amberdata.inbound.domain.FunctionCall;
import io.amberdata.inbound.stellar.client.HorizonServer;

import java.math.BigDecimal;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.stellar.sdk.AccountMergeOperation;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.operations.AccountMergeOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

public class AccountMergeOperationMapper implements OperationMapper {

  private static final Logger LOG = LoggerFactory.getLogger(AccountMergeOperationMapper.class);

  private final HorizonServer server;

  public AccountMergeOperationMapper(HorizonServer server) {
    this.server = server;
  }

  @Override
  @SuppressWarnings("checkstyle:MethodParamPad")
  public FunctionCall map(OperationResponse operationResponse) {
    AccountMergeOperationResponse response = (AccountMergeOperationResponse) operationResponse;

    final String from = response.getAccount();
    if (from == null || from == "") {
      LOG.warn("Source account in AccountMergeOperationResponse is null or empty");
    }

    final String into = response.getInto();
    if (into == null || into == "") {
      LOG.warn("Destination account in AccountMergeOperationResponse is null or empty");
    }

    BigDecimal lumensTransferred = BigDecimal.ZERO;
    final AccountResponse accountResponse = this.server.fetchAccountDetails(from);

    if (accountResponse != null) {
      for (AccountResponse.Balance balance : accountResponse.getBalances()) {
        if ("native".equals(balance.getAssetType())) {
          lumensTransferred = lumensTransferred.add(new BigDecimal(balance.getBalance()));
        }
      }
    }

    return new FunctionCall.Builder()
      .from             (this.fetchAccountId(from))
      .to               (this.fetchAccountId(into))
      .type             (AccountMergeOperation.class.getSimpleName())
      .lumensTransferred(lumensTransferred)
      .signature        ("account_merge(account_id)")
      .arguments        (
        Collections.singletonList(
          FunctionCall.Argument.from("destination", this.fetchAccountId(into))
        )
      )
      .build();
  }

  private String fetchAccountId(String account) {
    return account != null ? account : "";
  }

  @Override
  public List<Asset> getAssets(OperationResponse operationResponse) {
    return Collections.emptyList();
  }

}
