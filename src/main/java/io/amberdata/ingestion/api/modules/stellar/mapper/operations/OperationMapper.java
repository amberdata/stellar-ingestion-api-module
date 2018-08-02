package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.List;

import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.domain.Asset;
import io.amberdata.domain.FunctionCall;

public interface OperationMapper {

    FunctionCall map (OperationResponse operationResponse);
    List<Asset> getAssets (OperationResponse operationResponse);

}
