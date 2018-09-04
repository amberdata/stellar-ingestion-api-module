package io.amberdata.inbound.stellar.mapper.operations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stellar.sdk.CreatePassiveOfferOperation;
import org.stellar.sdk.responses.operations.CreatePassiveOfferOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.inbound.domain.Asset;
import io.amberdata.inbound.domain.FunctionCall;
import io.amberdata.inbound.stellar.mapper.AssetMapper;

public class CreatePassiveOfferOperationMapper implements OperationMapper {

    private static final Logger LOG = LoggerFactory.getLogger(CreatePassiveOfferOperationMapper.class);

    private AssetMapper assetMapper;

    public CreatePassiveOfferOperationMapper (AssetMapper assetMapper) {
        this.assetMapper = assetMapper;
    }

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        CreatePassiveOfferOperationResponse response = (CreatePassiveOfferOperationResponse) operationResponse;

        if (response.getSourceAccount() == null) {
            LOG.warn("Source account in CreatePassiveOfferOperationResponse is null");
        }

        Asset selling = assetMapper.map(response.getSellingAsset());
        Asset buying = assetMapper.map(response.getBuyingAsset());

        return new FunctionCall.Builder()
            .from(fetchAccountId(response))
            .type(CreatePassiveOfferOperation.class.getSimpleName())
            .value(response.getAmount())
            .meta(getOptionalProperties(response, selling, buying))
            .signature("create_passive_offer(asset, asset, integer, {numerator, denominator})")
            .arguments(Arrays.asList(
                    FunctionCall.Argument.from("selling", selling.getCode()),
                    FunctionCall.Argument.from("buying", buying.getCode()),
                    FunctionCall.Argument.from("amount", response.getAmount()),
                    FunctionCall.Argument.from("price", response.getPrice())
                )
            )
            .build();
    }

    private String fetchAccountId (CreatePassiveOfferOperationResponse response) {
        return response.getSourceAccount() != null ? response.getSourceAccount().getAccountId() : "";
    }

    @Override
    public List<Asset> getAssets (OperationResponse operationResponse) {
        CreatePassiveOfferOperationResponse response = (CreatePassiveOfferOperationResponse) operationResponse;

        Asset selling = assetMapper.map(response.getSellingAsset());
        Asset buying = assetMapper.map(response.getBuyingAsset());
        return Arrays.asList(selling, buying);
    }

    private Map<String, Object> getOptionalProperties (CreatePassiveOfferOperationResponse response,
                                                       Asset selling,
                                                       Asset buying) {
        Map<String, Object> optionalProperties = new HashMap<>();
        optionalProperties.put("sellingAsset", selling.getCode());
        optionalProperties.put("stellarSellingAssetType", selling.getType().getName());
        optionalProperties.put("sellingAssetIssuer", selling.getIssuerAccount());
        optionalProperties.put("buyingAsset", buying.getCode());
        optionalProperties.put("stellarBuyingAssetType", buying.getType().getName());
        optionalProperties.put("buyingAssetIssuer", buying.getIssuerAccount());
        optionalProperties.put("price", response.getPrice());
        return optionalProperties;
    }
}
