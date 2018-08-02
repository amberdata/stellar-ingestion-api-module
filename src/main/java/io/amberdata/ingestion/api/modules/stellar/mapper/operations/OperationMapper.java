package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.domain.FunctionCall;
import io.amberdata.domain.operations.Operation;

public interface OperationMapper {

    FunctionCall map (OperationResponse operationResponse);

}
