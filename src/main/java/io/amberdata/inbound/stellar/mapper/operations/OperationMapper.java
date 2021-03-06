package io.amberdata.inbound.stellar.mapper.operations;

import io.amberdata.inbound.domain.Asset;
import io.amberdata.inbound.domain.FunctionCall;

import java.util.List;

import org.stellar.sdk.responses.operations.OperationResponse;

public interface OperationMapper {

  FunctionCall map(OperationResponse operationResponse);

  List<Asset> getAssets(OperationResponse operationResponse);

}
