package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.domain.operations.Operation;

public interface OperationMapper {

    Operation map (OperationResponse operationResponse);

}
