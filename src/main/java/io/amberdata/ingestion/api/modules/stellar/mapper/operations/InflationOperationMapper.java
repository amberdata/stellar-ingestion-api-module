package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import org.stellar.sdk.responses.operations.InflationOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.domain.FunctionCall;
import io.amberdata.domain.operations.InflationOperation;
import io.amberdata.domain.operations.Operation;

public class InflationOperationMapper implements OperationMapper {

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        InflationOperationResponse response = (InflationOperationResponse) operationResponse;

        return new FunctionCall.Builder()
            .from(response.getSourceAccount().getAccountId())
            .build();
    }
}
