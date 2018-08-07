package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stellar.sdk.InflationOperation;
import org.stellar.sdk.responses.operations.InflationOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.domain.Asset;
import io.amberdata.domain.FunctionCall;

public class InflationOperationMapper implements OperationMapper {

    private static final Logger LOG = LoggerFactory.getLogger(InflationOperationMapper.class);

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        InflationOperationResponse response = (InflationOperationResponse) operationResponse;

        if (response.getSourceAccount() == null) {
            LOG.warn("Source account in InflationOperationResponse is null");
        }

        return new FunctionCall.Builder()
            .from(response.getSourceAccount() != null ? response.getSourceAccount().getAccountId() : "")
            .type(InflationOperation.class.getSimpleName())
            .build();
    }

    @Override
    public List<Asset> getAssets (OperationResponse operationResponse) {
        return Collections.emptyList();
    }
}
