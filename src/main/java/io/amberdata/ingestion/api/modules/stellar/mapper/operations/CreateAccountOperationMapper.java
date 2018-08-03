package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.Collections;
import java.util.List;

import org.stellar.sdk.responses.operations.AllowTrustOperationResponse;
import org.stellar.sdk.responses.operations.CreateAccountOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import com.google.common.base.Preconditions;

import io.amberdata.domain.Asset;
import io.amberdata.domain.FunctionCall;

public class CreateAccountOperationMapper implements OperationMapper {

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        CreateAccountOperationResponse response = (CreateAccountOperationResponse) operationResponse;

        Preconditions.checkNotNull(response.getFunder(), "Funder account in CreateAccountOperationResponse is null");
        Preconditions.checkNotNull(response.getAccount(), "Fundee account in CreateAccountOperationResponse is null");

        return new FunctionCall.Builder()
            .from(response.getFunder() != null ? response.getFunder().getAccountId() : null)
            .to(response.getAccount() != null ? response.getAccount().getAccountId() : null)
            .value(response.getStartingBalance())
            .build();
    }

    @Override
    public List<Asset> getAssets (OperationResponse operationResponse) {
        return Collections.emptyList();
    }
}
