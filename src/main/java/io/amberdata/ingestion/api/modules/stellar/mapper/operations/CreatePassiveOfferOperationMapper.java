package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import org.stellar.sdk.responses.operations.CreatePassiveOfferOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.domain.operations.CreatePassiveOfferOperation;
import io.amberdata.domain.operations.Operation;
import io.amberdata.ingestion.api.modules.stellar.mapper.AssetMapper;

public class CreatePassiveOfferOperationMapper implements OperationMapper {

    private AssetMapper assetMapper;

    public CreatePassiveOfferOperationMapper (AssetMapper assetMapper) {
        this.assetMapper = assetMapper;
    }

    @Override
    public Operation map (OperationResponse operationResponse) {
        CreatePassiveOfferOperationResponse response = (CreatePassiveOfferOperationResponse) operationResponse;

        return new CreatePassiveOfferOperation(
            response.getSourceAccount().getAccountId(),
            assetMapper.map(response.getSellingAsset()),
            assetMapper.map(response.getBuyingAsset()),
            response.getAmount(),
            response.getPrice()
        );
    }
}
