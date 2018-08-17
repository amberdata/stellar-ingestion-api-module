package io.amberdata.ingestion.stellar.mapper.operations;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.stellar.sdk.BumpSequenceOperation;
import org.stellar.sdk.responses.operations.BumpSequenceOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.ingestion.domain.Asset;
import io.amberdata.ingestion.domain.FunctionCall;

public class BumpSequenceOperationMapper implements OperationMapper {

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        BumpSequenceOperationResponse response = (BumpSequenceOperationResponse) operationResponse;

        return new FunctionCall.Builder()
            .from(fetchAccountId(response))
            .type(BumpSequenceOperation.class.getSimpleName())
            .meta(getOptionalProperties(response))
            .signature("bump_sequence(sequence_number)")
            .arguments(Collections.singletonList(
                    FunctionCall.Argument.from("bump_to",
                        response.getBumpTo() != null ? response.getBumpTo().toString() : "")
                )
            )
            .build();
    }

    @Override
    public List<Asset> getAssets (OperationResponse operationResponse) {
        return Collections.emptyList();
    }

    private String fetchAccountId (BumpSequenceOperationResponse response) {
        return response.getSourceAccount() != null ? response.getSourceAccount().getAccountId() : "";
    }

    private Map<String, Object> getOptionalProperties (BumpSequenceOperationResponse response) {
        Map<String, Object> optionalProperties = new HashMap<>();
        optionalProperties.put("bumpTo", response.getBumpTo() != null ? response.getBumpTo().toString() : "");
        return optionalProperties;
    }
}