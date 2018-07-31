package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import org.stellar.sdk.responses.operations.ManageOfferOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.domain.operations.ManageOfferOperation;
import io.amberdata.domain.operations.Operation;
import io.amberdata.ingestion.api.modules.stellar.mapper.AssetMapper;

public class ManageOfferOperationMapper implements OperationMapper {

    private AssetMapper assetMapper;

    public ManageOfferOperationMapper (AssetMapper assetMapper) {
        this.assetMapper = assetMapper;
    }

    @Override
    public Operation map (OperationResponse operationResponse) {
        ManageOfferOperationResponse response = (ManageOfferOperationResponse) operationResponse;

        return new ManageOfferOperation(
            response.getSourceAccount().getAccountId(),
            assetMapper.map(response.getSellingAsset()),
            assetMapper.map(response.getBuyingAsset()),
            response.getAmount(),
            response.getPrice(),
            response.getOfferId()
        );
    }
}
