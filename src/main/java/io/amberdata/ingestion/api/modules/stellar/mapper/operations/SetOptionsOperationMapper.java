package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.SetOptionsOperationResponse;

import io.amberdata.domain.operations.Operation;
import io.amberdata.domain.operations.SetOptionsOperation;

public class SetOptionsOperationMapper implements OperationMapper {

    @Override
    public Operation map (OperationResponse operationResponse) {
        SetOptionsOperationResponse response = (SetOptionsOperationResponse) operationResponse;

        return new SetOptionsOperation(
            response.getSourceAccount().getAccountId(),
            response.getInflationDestination().getAccountId(),
            response.getClearFlags(),
            response.getSetFlags(),
            response.getMasterKeyWeight(),
            response.getLowThreshold(),
            response.getMedThreshold(),
            response.getHighThreshold(),
            response.getHomeDomain(),
            response.getSigner().getAccountId(),
            response.getSignerWeight()
        );
    }
}
