package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stellar.sdk.SetOptionsOperation;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.SetOptionsOperationResponse;

import io.amberdata.domain.Asset;
import io.amberdata.domain.FunctionCall;

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

        if (response.getSigner() == null) {
            LOG.warn("Signer account in SetOptionsOperationResponse is null");
        }

        return new FunctionCall.Builder()
            .from(response.getSourceAccount() != null ? response.getSourceAccount().getAccountId() : "")
            .to(response.getInflationDestination() != null ? response.getInflationDestination().getAccountId() : "")
            .type(SetOptionsOperation.class.getSimpleName())
            .signature(response.getSigner() != null ? response.getSigner().getAccountId() : "")
            .optionalProperties(getOptionalProperties(response))
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
        optionalProperties.put("signerWeight", response.getSignerWeight());
        return optionalProperties;
    }
}
