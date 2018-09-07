package io.amberdata.inbound.stellar.mapper.operations;

import io.amberdata.inbound.domain.Asset;
import io.amberdata.inbound.domain.FunctionCall;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.stellar.sdk.CreateAccountOperation;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.responses.operations.CreateAccountOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CreateAccountOperationMapper implements OperationMapper {

  private static final Logger LOG = LoggerFactory.getLogger(CreateAccountOperationMapper.class);

  @Override
  public FunctionCall map(OperationResponse operationResponse) {
    CreateAccountOperationResponse response = (CreateAccountOperationResponse) operationResponse;

    if (response.getFunder() == null) {
      LOG.warn("Funder account in CreateAccountOperationResponse is null");
    }

    if (response.getAccount() == null) {
      LOG.warn("Fundee account in CreateAccountOperationResponse is null");
    }

    return new FunctionCall.Builder()
        .from(fetchAccountId(response.getFunder()))
        .to(fetchAccountId(response.getAccount()))
        .type(CreateAccountOperation.class.getSimpleName())
        .value(response.getStartingBalance())
        .signature("create_account(account_id, integer)")
        .arguments(
            Arrays.asList(
                FunctionCall.Argument.from("destination", fetchAccountId(response.getAccount())),
                FunctionCall.Argument.from("starting_balance", response.getStartingBalance())
            )
        )
        .build();
  }

  private String fetchAccountId(KeyPair funder) {
    return funder != null ? funder.getAccountId() : "";
  }

  @Override
  public List<Asset> getAssets(OperationResponse operationResponse) {
    return Collections.emptyList();
  }
}
