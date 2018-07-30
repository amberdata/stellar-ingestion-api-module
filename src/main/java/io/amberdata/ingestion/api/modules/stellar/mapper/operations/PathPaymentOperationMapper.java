package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import org.springframework.beans.factory.annotation.Autowired;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PathPaymentOperationResponse;
import org.stellar.sdk.xdr.PathPaymentOp;

import io.amberdata.domain.operations.Operation;
import io.amberdata.domain.operations.PathPaymentOperation;
import io.amberdata.ingestion.api.modules.stellar.mapper.AssetMapper;

public class PathPaymentOperationMapper implements OperationMapper {

    private AssetMapper assetMapper;

    public PathPaymentOperationMapper (AssetMapper assetMapper) {
        this.assetMapper = assetMapper;
    }

    @Override
    public Operation map (OperationResponse operationResponse) {
        PathPaymentOperationResponse response = (PathPaymentOperationResponse) operationResponse;

        return new PathPaymentOperation(
            response.getFrom().getAccountId(),
            assetMapper.map(response.getSourceAsset()),
            response.getSourceMax(),
            response.getTo().getAccountId(),
            assetMapper.map(response.getAsset()),
            response.getAmount(),
            null //TODO where is the path?
        );
    }
}
