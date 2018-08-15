package io.amberdata.ingestion.stellar.mapper.operations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stellar.sdk.ManageDataOperation;
import org.stellar.sdk.responses.operations.ManageDataOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.ingestion.domain.Asset;
import io.amberdata.ingestion.domain.FunctionCall;

public class ManageDataOperationMapper implements OperationMapper {

    private static final Logger LOG = LoggerFactory.getLogger(ManageDataOperationMapper.class);

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        ManageDataOperationResponse response = (ManageDataOperationResponse) operationResponse;

        if (response.getSourceAccount() == null) {
            LOG.warn("Source account in ManageDataOperationResponse is null");
        }

        return new FunctionCall.Builder()
            .from(fetchAccountId(response))
            .type(ManageDataOperation.class.getSimpleName())
            .signature("manage_data(string, binary_data)")
            .arguments(
                Arrays.asList(
                    FunctionCall.Argument.from("name", response.getName()),
                    FunctionCall.Argument.from("value", response.getValue())
                )
            )
            .build();
    }

    private String fetchAccountId (ManageDataOperationResponse response) {
        return response.getSourceAccount() != null ? response.getSourceAccount().getAccountId() : "";
    }

    @Override
    public List<Asset> getAssets (OperationResponse operationResponse) {
        return Collections.emptyList();
    }
}
