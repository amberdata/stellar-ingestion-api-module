package io.amberdata.ingestion.stellar.mapper.operations;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.ingestion.domain.Asset;
import io.amberdata.ingestion.domain.FunctionCall;
import io.amberdata.ingestion.stellar.client.PreAuthOperationResponse;
import io.amberdata.ingestion.stellar.mapper.operations.OperationMapper;

public class PreAuthOperationMapper implements OperationMapper {

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        PreAuthOperationResponse response = (PreAuthOperationResponse) operationResponse;

        return new FunctionCall.Builder()
            .from(response.getPreAuthSourceAccount())
            .type("PreAuthOperation")
            .optionalProperties(getOptionalProperties(response))
            .signature("set_options(account_id, integer, integer, integer, integer," +
                "integer, integer, string, {public_key, weight})")
            .arguments(
                Arrays.asList(
                    FunctionCall.Argument.from("inflation_destination", ""),
                    FunctionCall.Argument.from("clear_flags", ""),
                    FunctionCall.Argument.from("set_flags", ""),
                    FunctionCall.Argument.from("master_weight", ""),
                    FunctionCall.Argument.from("low_threshold", ""),
                    FunctionCall.Argument.from("medium_threshold", ""),
                    FunctionCall.Argument.from("high_threshold", ""),
                    FunctionCall.Argument.from("home_domain", ""),
                    FunctionCall.Argument.from("signer", "{" + response.getSignerKey() + "," +
                        response.getSignerWeight() + "}")
                )
            )
            .build();
    }

    @Override
    public List<Asset> getAssets (OperationResponse operationResponse) {
        return Collections.emptyList();
    }

    private Map<String, Object> getOptionalProperties (PreAuthOperationResponse response) {
        Map<String, Object> optionalProperties = new HashMap<>();
        optionalProperties.put("signer", response.getSignerKey());
        optionalProperties.put("signerWeight", response.getSignerWeight());
        return optionalProperties;
    }
}
