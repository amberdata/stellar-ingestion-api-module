package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import org.stellar.sdk.responses.operations.CreateAccountOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.domain.operations.CreateAccountOperation;
import io.amberdata.domain.operations.Operation;

public class CreateAccountOperationMapper implements OperationMapper {

    @Override
    public Operation map (OperationResponse operationResponse) {
        CreateAccountOperationResponse response = (CreateAccountOperationResponse) operationResponse;

        return new CreateAccountOperation(
            response.getFunder().getAccountId(),
            response.getAccount().getAccountId(),
            response.getStartingBalance()
        );
    }
}
