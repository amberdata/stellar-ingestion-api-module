package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import org.stellar.sdk.responses.operations.AllowTrustOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.domain.operations.AllowTrustOperation;
import io.amberdata.domain.operations.Operation;
import io.amberdata.ingestion.api.modules.stellar.mapper.AssetMapper;

public class AllowTrustOperationMapper implements OperationMapper {

    private AssetMapper assetMapper;

    public AllowTrustOperationMapper (AssetMapper assetMapper) {
        this.assetMapper = assetMapper;
    }

    @Override
    public Operation map (OperationResponse operationResponse) {
        AllowTrustOperationResponse response = (AllowTrustOperationResponse) operationResponse;

        return new AllowTrustOperation(
            response.getTrustee().getAccountId(),
            response.getTrustor().getAccountId(),
            assetMapper.map(response.getAsset()),
            response.isAuthorize()
        );
    }
}
