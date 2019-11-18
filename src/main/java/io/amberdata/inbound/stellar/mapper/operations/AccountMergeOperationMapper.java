package io.amberdata.inbound.stellar.mapper.operations;

import io.amberdata.inbound.domain.Asset;
import io.amberdata.inbound.domain.FunctionCall;
import io.amberdata.inbound.stellar.client.HorizonServer;

import java.io.IOException;

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

    if (response.getAccount() == null) {
      LOG.warn("Source account in AccountMergeOperationResponse is null");
    }

    if (response.getInto() == null) {
      LOG.warn("Destination account in AccountMergeOperationResponse is null");
    }

    BigDecimal lumensTransferred = BigDecimal.ZERO;
    try {
      AccountResponse accountResponse = this.server.horizonServer()
          .accounts()
          .account(response.getAccount());

      for (AccountResponse.Balance balance : accountResponse.getBalances()) {
        if ("native".equals(balance.getAssetType())) {
          lumensTransferred = lumensTransferred.add(new BigDecimal(balance.getBalance()));
        }
      }
    } catch (IOException ioe) {
      // throw new HorizonServer.StellarException(ioe.getMessage(), ioe);
      LOG.warn(ioe.getMessage(), ioe);
      lumensTransferred = BigDecimal.ZERO;
    }

    return new FunctionCall.Builder()
      .from             (this.fetchAccountId(response.getAccount()))
      .to               (this.fetchAccountId(response.getInto()))
      .type             (AccountMergeOperation.class.getSimpleName())
      .lumensTransferred(lumensTransferred)
      .signature        ("account_merge(account_id)")
      .arguments        (
        Collections.singletonList(
          FunctionCall.Argument.from("destination", this.fetchAccountId(response.getInto()))
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
