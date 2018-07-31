package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;

import io.amberdata.domain.operations.Operation;
import io.amberdata.domain.operations.PaymentOperation;
import io.amberdata.ingestion.api.modules.stellar.mapper.AssetMapper;

public class PaymentOperationMapper implements OperationMapper {

    private AssetMapper assetMapper;

    public PaymentOperationMapper (AssetMapper assetMapper) {
        this.assetMapper = assetMapper;
    }

    @Override
    public Operation map (OperationResponse operationResponse) {
        PaymentOperationResponse response = (PaymentOperationResponse) operationResponse;

        return new PaymentOperation(
            response.getFrom().getAccountId(),
            response.getTo().getAccountId(),
            this.assetMapper.map(response.getAsset()),
            response.getAmount()
        );
    }
}
