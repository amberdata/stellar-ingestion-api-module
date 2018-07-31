package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import org.stellar.sdk.responses.operations.ManageDataOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.domain.operations.ManageDataOperation;
import io.amberdata.domain.operations.Operation;

public class ManageDataOperationMapper implements OperationMapper {

    @Override
    public Operation map (OperationResponse operationResponse) {
        ManageDataOperationResponse response = (ManageDataOperationResponse) operationResponse;

        return new ManageDataOperation(
            response.getSourceAccount().getAccountId(),
            response.getName(),
            response.getValue()
        );
    }
}
