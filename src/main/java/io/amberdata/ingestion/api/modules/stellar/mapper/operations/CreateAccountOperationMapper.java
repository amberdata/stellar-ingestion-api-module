package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stellar.sdk.responses.operations.CreateAccountOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.domain.Asset;
import io.amberdata.domain.FunctionCall;

public class CreateAccountOperationMapper implements OperationMapper {

    private static final Logger LOG = LoggerFactory.getLogger(CreateAccountOperationMapper.class);

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        CreateAccountOperationResponse response = (CreateAccountOperationResponse) operationResponse;

        if (response.getFunder() == null) {
            LOG.warn("Funder account in CreateAccountOperationResponse is null");
        }

        if (response.getAccount() == null) {
            LOG.warn("Fundee account in CreateAccountOperationResponse is null");
        }

        return new FunctionCall.Builder()
            .from(response.getFunder() != null ? response.getFunder().getAccountId() : "")
            .to(response.getAccount() != null ? response.getAccount().getAccountId() : "")
            .value(response.getStartingBalance())
            .build();
    }

    @Override
    public List<Asset> getAssets (OperationResponse operationResponse) {
        return Collections.emptyList();
    }
}
