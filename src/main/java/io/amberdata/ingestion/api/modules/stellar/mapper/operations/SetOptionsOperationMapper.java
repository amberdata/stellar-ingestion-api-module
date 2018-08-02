package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.HashMap;
import java.util.Map;

import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.SetOptionsOperationResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.amberdata.domain.FunctionCall;

public class SetOptionsOperationMapper implements OperationMapper {

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        SetOptionsOperationResponse response = (SetOptionsOperationResponse) operationResponse;

        return new FunctionCall.Builder()
            .from(response.getSourceAccount().getAccountId())
            .to(response.getInflationDestination().getAccountId())
            .signature(response.getSigner().getAccountId())
            .meta(getMetaProperties(response))
            .build();
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
            return null;
        }
    }
}
