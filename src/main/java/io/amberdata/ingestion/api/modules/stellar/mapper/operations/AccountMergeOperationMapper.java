package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.Collections;
import java.util.List;

import org.stellar.sdk.responses.operations.AccountMergeOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.domain.Asset;
import io.amberdata.domain.FunctionCall;

public class AccountMergeOperationMapper implements OperationMapper {

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        AccountMergeOperationResponse response = (AccountMergeOperationResponse) operationResponse;

        return new FunctionCall.Builder()
            .from(response.getAccount().getAccountId())
            .to(response.getInto().getAccountId())
            .build();
    }

    @Override
    public List<Asset> getAssets (OperationResponse operationResponse) {
        return Collections.emptyList();
    }
}
