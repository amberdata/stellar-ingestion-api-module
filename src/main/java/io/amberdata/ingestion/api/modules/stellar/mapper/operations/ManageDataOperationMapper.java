package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.Collections;
import java.util.List;

import org.stellar.sdk.responses.operations.ManageDataOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import com.google.common.base.Preconditions;

import io.amberdata.domain.Asset;
import io.amberdata.domain.FunctionCall;

public class ManageDataOperationMapper implements OperationMapper {

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        ManageDataOperationResponse response = (ManageDataOperationResponse) operationResponse;

        Preconditions.checkNotNull(
            response.getSourceAccount(),
            "Source account in ManageDataOperationResponse is null"
        );

        return new FunctionCall.Builder()
            .from(response.getSourceAccount() != null ? response.getSourceAccount().getAccountId() : null)
            .arguments(
                Collections.singletonList(
                    FunctionCall.Argument.from(response.getName(), response.getValue())
                )
            )
            .build();
    }

    @Override
    public List<Asset> getAssets (OperationResponse operationResponse) {
        return Collections.emptyList();
    }
}
