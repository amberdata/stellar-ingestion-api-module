package io.amberdata.inbound.stellar.mapper.operations;

import io.amberdata.inbound.domain.Asset;
import io.amberdata.inbound.domain.FunctionCall;

import java.math.BigDecimal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.stellar.sdk.CreateAccountOperation;
import org.stellar.sdk.responses.operations.CreateAccountOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

public class CreateAccountOperationMapper implements OperationMapper {

  private static final Logger LOG = LoggerFactory.getLogger(CreateAccountOperationMapper.class);

  @Override
  @SuppressWarnings("checkstyle:MethodParamPad")
  public FunctionCall map(OperationResponse operationResponse) {
    CreateAccountOperationResponse response = (CreateAccountOperationResponse) operationResponse;

    if (response.getFunder() == null) {
      LOG.warn("Funder account in CreateAccountOperationResponse is null");
    }

    if (response.getAccount() == null) {
      LOG.warn("Fundee account in CreateAccountOperationResponse is null");
    }

    String to = this.fetchAccountId(response.getAccount());

    return new FunctionCall.Builder()
        .from             (this.fetchAccountId(response.getFunder()))
        .to               (to)
        .type             (CreateAccountOperation.class.getSimpleName())
        .value            (response.getStartingBalance())
        .lumensTransferred(new BigDecimal(response.getStartingBalance()))
        .signature        ("create_account(account_id, integer)")
        .arguments        (
            Arrays.asList(
                FunctionCall.Argument.from("destination",      to),
                FunctionCall.Argument.from("starting_balance", response.getStartingBalance())
            )
        )
        .build();
  }

  private String fetchAccountId(String funder) {
    return funder != null ? funder : "";
  }

  @Override
  public List<Asset> getAssets(OperationResponse operationResponse) {
    return Collections.emptyList();
  }

}
