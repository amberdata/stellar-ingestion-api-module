package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.Collections;
import java.util.List;

import org.stellar.sdk.responses.operations.AccountMergeOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import com.google.common.base.Preconditions;

import io.amberdata.domain.Asset;
import io.amberdata.domain.FunctionCall;

public class AccountMergeOperationMapper implements OperationMapper {

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        AccountMergeOperationResponse response = (AccountMergeOperationResponse) operationResponse;

        Preconditions.checkNotNull(response.getAccount(), "Source account in AccountMergeOperationResponse is null");
        Preconditions.checkNotNull(response.getInto(), "Destination account in AccountMergeOperationResponse is null");

        return new FunctionCall.Builder()
            .from(response.getAccount() != null ? response.getAccount().getAccountId() : null)
            .to(response.getInto() != null ? response.getInto().getAccountId() : null)
            .build();
    }

    @Override
    public List<Asset> getAssets (OperationResponse operationResponse) {
        return Collections.emptyList();
    }
}
