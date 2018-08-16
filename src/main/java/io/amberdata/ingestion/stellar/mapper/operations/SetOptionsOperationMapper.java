package io.amberdata.ingestion.stellar.mapper.operations;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.SetOptionsOperation;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.SetOptionsOperationResponse;

import io.amberdata.ingestion.domain.Asset;
import io.amberdata.ingestion.domain.FunctionCall;

public class SetOptionsOperationMapper implements OperationMapper {

    private static final Logger LOG = LoggerFactory.getLogger(SetOptionsOperationMapper.class);

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        SetOptionsOperationResponse response = (SetOptionsOperationResponse) operationResponse;

        if (response.getSourceAccount() == null) {
            LOG.warn("Source account in SetOptionsOperationResponse is null");
        }

        if (response.getInflationDestination() == null) {
            LOG.warn("Inflation destination account in SetOptionsOperationResponse is null");
        }

        return new FunctionCall.Builder()
            .from(fetchAccountId(response.getSourceAccount()))
            .to(fetchAccountId(response.getInflationDestination()))
            .type(SetOptionsOperation.class.getSimpleName())
            .meta(getOptionalProperties(response))
            .signature("set_options(account_id, integer, integer, integer, integer," +
                "integer, integer, string, {public_key, weight})")
            .arguments(
                Arrays.asList(
                    FunctionCall.Argument.from("inflation_destination",
                        fetchAccountId(response.getInflationDestination())),

                    FunctionCall.Argument.from("clear_flags",
                        response.getClearFlags() != null && response.getClearFlags().length > 0 ?
                            String.join("-", response.getClearFlags()) : ""),

                    FunctionCall.Argument.from("set_flags",
                        response.getSetFlags() != null && response.getSetFlags().length > 0 ?
                            String.join("-", response.getSetFlags()) : ""),

                    FunctionCall.Argument.from("master_weight", Objects.toString(response.getMasterKeyWeight(), "")),
                    FunctionCall.Argument.from("low_threshold", Objects.toString(response.getLowThreshold(), "")),
                    FunctionCall.Argument.from("medium_threshold", Objects.toString(response.getMedThreshold(), "")),
                    FunctionCall.Argument.from("high_threshold", Objects.toString(response.getHighThreshold(), "")),
                    FunctionCall.Argument.from("home_domain", response.getHomeDomain()),
                    FunctionCall.Argument.from("signer", "{" + response.getSignerKey() +
                        "," + response.getSignerWeight() + "}")
                )
            )
            .build();
    }

    @Override
    public List<Asset> getAssets (OperationResponse operationResponse) {
        return Collections.emptyList();
    }

    private Map<String, Object> getOptionalProperties (SetOptionsOperationResponse response) {
        Map<String, Object> optionalProperties = new HashMap<>();
        if (response.getClearFlags() != null && response.getClearFlags().length > 0) {
            optionalProperties.put("clearFlags", String.join("-", response.getClearFlags()));
        }
        if (response.getSetFlags() != null && response.getSetFlags().length > 0) {
            optionalProperties.put("setFlags", String.join("-", response.getSetFlags()));
        }
        optionalProperties.put("masterKeyWeight", response.getMasterKeyWeight());
        optionalProperties.put("lowThreshold", response.getLowThreshold());
        optionalProperties.put("medThreshold", response.getMedThreshold());
        optionalProperties.put("highThreshold", response.getHighThreshold());
        optionalProperties.put("homeDomain", response.getHomeDomain());
        optionalProperties.put("signer", response.getSignerKey());
        optionalProperties.put("signerWeight", response.getSignerWeight());
        return optionalProperties;
    }

    private String fetchAccountId (KeyPair sourceAccount) {
        return sourceAccount != null ? sourceAccount.getAccountId() : "";
    }
}
