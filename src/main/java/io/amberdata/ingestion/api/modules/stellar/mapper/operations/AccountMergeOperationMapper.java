package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import org.stellar.sdk.responses.operations.AccountMergeOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.domain.operations.AccountMergeOperation;
import io.amberdata.domain.operations.Operation;

public class AccountMergeOperationMapper implements OperationMapper {

    @Override
    public Operation map (OperationResponse operationResponse) {
        AccountMergeOperationResponse response = (AccountMergeOperationResponse) operationResponse;

        return new AccountMergeOperation(
            response.getAccount().getAccountId(),
            response.getInto().getAccountId()
        );
    }
}