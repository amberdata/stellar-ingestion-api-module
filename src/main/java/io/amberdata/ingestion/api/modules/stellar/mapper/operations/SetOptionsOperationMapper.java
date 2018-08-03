package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.SetOptionsOperationResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

import io.amberdata.domain.Asset;
import io.amberdata.domain.FunctionCall;

public class SetOptionsOperationMapper implements OperationMapper {

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        SetOptionsOperationResponse response = (SetOptionsOperationResponse) operationResponse;

        Preconditions.checkNotNull(
            response.getSourceAccount(),
            "Source account in SetOptionsOperationResponse is null"
        );
        Preconditions.checkNotNull(
            response.getInflationDestination(),
            "Inflation destination account in SetOptionsOperationResponse is null"
        );
        Preconditions.checkNotNull(
            response.getSigner(),
            "Signer account in SetOptionsOperationResponse is null"
        );

        return new FunctionCall.Builder()
            .from(response.getSourceAccount() != null ? response.getSourceAccount().getAccountId() : null)
            .to(response.getInflationDestination() != null ? response.getInflationDestination().getAccountId() : null)
            .signature(response.getSigner() != null ? response.getSigner().getAccountId() : null)
            .meta(getMetaProperties(response))
            .build();
    }

    @Override
    public List<Asset> getAssets (OperationResponse operationResponse) {
        return Collections.emptyList();
    }

    private String getMetaProperties (SetOptionsOperationResponse response) {
        Map<String, String> metaMap = new HashMap<>();
        metaMap.put("clearFlags", String.join("-", response.getClearFlags()));
        metaMap.put("setFlags", String.join("-", response.getSetFlags()));
        metaMap.put("masterKeyWeight", response.getMasterKeyWeight().toString());
        metaMap.put("lowThreshold", response.getLowThreshold().toString());
        metaMap.put("medThreshold", response.getMedThreshold().toString());
        metaMap.put("highThreshold", response.getHighThreshold().toString());
        metaMap.put("homeDomain", response.getHomeDomain());
        metaMap.put("signerWeight", response.getSignerWeight().toString());
        try {
            return new ObjectMapper().writeValueAsString(metaMap);
        }
        catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
