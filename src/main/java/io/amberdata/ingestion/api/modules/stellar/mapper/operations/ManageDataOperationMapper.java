package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.Collections;

import org.stellar.sdk.responses.operations.ManageDataOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.domain.FunctionCall;

public class ManageDataOperationMapper implements OperationMapper {

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        ManageDataOperationResponse response = (ManageDataOperationResponse) operationResponse;

        return new FunctionCall.Builder()
            .from(response.getSourceAccount().getAccountId())
            .arguments(
                Collections.singletonList(
                    FunctionCall.Argument.from(response.getName(), response.getValue())
                )
            )
            .build();
    }
}
