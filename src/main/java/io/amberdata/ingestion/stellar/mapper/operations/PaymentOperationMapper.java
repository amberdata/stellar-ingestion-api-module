package io.amberdata.ingestion.stellar.mapper.operations;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.PaymentOperation;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;

import io.amberdata.ingestion.domain.Asset;
import io.amberdata.ingestion.domain.FunctionCall;
import io.amberdata.ingestion.stellar.mapper.AssetMapper;

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
            .from(fetchAccountId(response.getFrom()))
            .to(fetchAccountId(response.getTo()))
            .type(PaymentOperation.class.getSimpleName())
            .assetType(asset.getCode())
            .value(response.getAmount())
            .meta(getOptionalProperties(asset))
            .signature("payment(account_id, asset, integer)")
            .arguments(
                Arrays.asList(
                    FunctionCall.Argument.from("destination", fetchAccountId(response.getTo())),
                    FunctionCall.Argument.from("asset", asset.getCode()),
                    FunctionCall.Argument.from("amount", response.getAmount())
                )
            )
            .build();
    }

    private String fetchAccountId (KeyPair from) {
        return from != null ? from.getAccountId() : "";
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
