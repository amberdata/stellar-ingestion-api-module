package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import org.stellar.sdk.responses.operations.ChangeTrustOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.domain.operations.ChangeTrustOperation;
import io.amberdata.domain.operations.Operation;
import io.amberdata.ingestion.api.modules.stellar.mapper.AssetMapper;

public class ChangeTrustOperationMapper implements OperationMapper {

    private AssetMapper assetMapper;

    public ChangeTrustOperationMapper (AssetMapper assetMapper) {
        this.assetMapper = assetMapper;
    }

    @Override
    public Operation map (OperationResponse operationResponse) {
        ChangeTrustOperationResponse response = (ChangeTrustOperationResponse) operationResponse;

        return new ChangeTrustOperation(
            response.getTrustor().getAccountId(),
            response.getTrustee().getAccountId(),
            assetMapper.map(response.getAsset()),
            response.getLimit()
        );
    }
}
