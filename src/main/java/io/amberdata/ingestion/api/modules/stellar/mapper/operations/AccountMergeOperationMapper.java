package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stellar.sdk.responses.operations.AccountMergeOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.domain.Asset;
import io.amberdata.domain.FunctionCall;

public class AccountMergeOperationMapper implements OperationMapper {

    private static final Logger LOG = LoggerFactory.getLogger(AccountMergeOperationMapper.class);

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        AccountMergeOperationResponse response = (AccountMergeOperationResponse) operationResponse;

        if (response.getAccount() == null) {
            LOG.warn("Source account in AccountMergeOperationResponse is null");
        }

        if (response.getInto() == null) {
            LOG.warn("Destination account in AccountMergeOperationResponse is null");
        }

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
