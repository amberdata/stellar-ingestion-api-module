package io.amberdata.ingestion.stellar.mapper.operations;

import java.util.List;

import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.ingestion.domain.Asset;
import io.amberdata.ingestion.domain.FunctionCall;

public interface OperationMapper {

    FunctionCall map (OperationResponse operationResponse);
    List<Asset> getAssets (OperationResponse operationResponse);

}
