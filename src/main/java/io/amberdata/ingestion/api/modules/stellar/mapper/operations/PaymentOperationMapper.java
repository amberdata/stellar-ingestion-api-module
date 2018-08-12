package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stellar.sdk.PaymentOperation;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.amberdata.domain.Asset;
import io.amberdata.domain.FunctionCall;
import io.amberdata.ingestion.api.modules.stellar.mapper.AssetMapper;

public class PaymentOperationMapper implements OperationMapper {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentOperationMapper.class);

    private AssetMapper assetMapper;

    public PaymentOperationMapper (AssetMapper assetMapper) {
        this.assetMapper = assetMapper;
    }

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        PaymentOperationResponse response = (PaymentOperationResponse) operationResponse;

        if (response.getAsset() == null) {
            LOG.warn("Asset in PaymentOperationResponse is null");
        }

        if (response.getFrom() == null) {
            LOG.warn("Source account in PaymentOperationResponse is null");
        }

        if (response.getTo() == null) {
            LOG.warn("Destination account in PaymentOperationResponse is null");
        }

        Asset asset = this.assetMapper.map(response.getAsset());

        return new FunctionCall.Builder()
            .from(response.getFrom() != null ? response.getFrom().getAccountId() : "")
            .to(response.getTo() != null ? response.getTo().getAccountId() : "")
            .type(PaymentOperation.class.getSimpleName())
            .assetType(asset.getCode())
            .value(response.getAmount())
            .optionalProperties(getOptionalProperties(asset))
            .signature("payment(account_id, asset, integer)")
            .arguments(
                Arrays.asList(
                    FunctionCall.Argument.from("destination", response.getTo().getAccountId()),
                    FunctionCall.Argument.from("asset", asset.getCode()),
                    FunctionCall.Argument.from("amount", response.getAmount())
                )
            )
            .build();
    }

    @Override
    public List<Asset> getAssets (OperationResponse operationResponse) {
        PaymentOperationResponse response = (PaymentOperationResponse) operationResponse;

        Asset asset = assetMapper.map(response.getAsset());
        return Collections.singletonList(asset);
    }

    private Map<String, Object> getOptionalProperties (Asset asset) {
        Map<String, Object> optionalProperties = new HashMap<>();
        optionalProperties.put("stellarAssetType", asset.getType().getName());
        optionalProperties.put("assetIssuer", asset.getIssuerAccount());
        return optionalProperties;
    }
}
