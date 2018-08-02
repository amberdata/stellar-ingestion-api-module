package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;

import io.amberdata.domain.Asset;
import io.amberdata.domain.FunctionCall;
import io.amberdata.domain.operations.Operation;
import io.amberdata.domain.operations.PaymentOperation;
import io.amberdata.ingestion.api.modules.stellar.mapper.AssetMapper;

public class PaymentOperationMapper implements OperationMapper {

    private AssetMapper assetMapper;

    public PaymentOperationMapper (AssetMapper assetMapper) {
        this.assetMapper = assetMapper;
    }

    @Override
    public FunctionCall map (OperationResponse operationResponse) {
        PaymentOperationResponse response = (PaymentOperationResponse) operationResponse;

        Asset asset = this.assetMapper.map(response.getAsset());

        return new FunctionCall.Builder()
            .from(response.getFrom().getAccountId())
            .to(response.getTo().getAccountId())
            .assetType(asset.getCode())
            .value(response.getAmount())
            .build();
    }
}
