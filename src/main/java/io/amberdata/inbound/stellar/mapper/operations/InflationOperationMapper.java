package io.amberdata.inbound.stellar.mapper.operations;

import io.amberdata.inbound.domain.Asset;
import io.amberdata.inbound.domain.FunctionCall;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.stellar.sdk.InflationOperation;
import org.stellar.sdk.responses.operations.InflationOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

public class InflationOperationMapper implements OperationMapper {
  private static final Logger LOG = LoggerFactory.getLogger(InflationOperationMapper.class);

  @Override
  public FunctionCall map(OperationResponse operationResponse) {
    InflationOperationResponse response = (InflationOperationResponse) operationResponse;

    if (response.getSourceAccount() == null) {
      LOG.warn("Source account in InflationOperationResponse is null");
    }

    return new FunctionCall.Builder()
        .from(this.fetchAccountId(response))
        .type(InflationOperation.class.getSimpleName())
        .signature("inflation()")
        .arguments(Collections.emptyList())
        .build();
  }

  private String fetchAccountId(InflationOperationResponse response) {
    return response.getSourceAccount() != null ? response.getSourceAccount() : "";
  }

  @Override
  public List<Asset> getAssets(OperationResponse operationResponse) {
    return Collections.emptyList();
  }

}
