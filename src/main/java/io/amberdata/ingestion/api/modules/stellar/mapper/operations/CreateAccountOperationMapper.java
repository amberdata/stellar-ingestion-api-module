package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.Collections;
import java.util.List;

import org.stellar.sdk.responses.operations.AllowTrustOperationResponse;
import org.stellar.sdk.responses.operations.CreateAccountOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.domain.Asset;
import io.amberdata.domain.FunctionCall;

public class CreateAccountOperationMapper implements OperationMapper {

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        CreateAccountOperationResponse response = (CreateAccountOperationResponse) operationResponse;

        return new FunctionCall.Builder()
            .from(response.getFunder().getAccountId())
            .to(response.getAccount().getAccountId())
            .value(response.getStartingBalance())
            .build();
    }

    @Override
    public List<Asset> getAssets (OperationResponse operationResponse) {
        return Collections.emptyList();
    }
}
