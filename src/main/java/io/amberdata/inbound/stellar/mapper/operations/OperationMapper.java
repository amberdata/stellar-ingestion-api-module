package io.amberdata.inbound.stellar.mapper.operations;

import java.util.List;

import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.inbound.domain.Asset;
import io.amberdata.inbound.domain.FunctionCall;

public interface OperationMapper {

    FunctionCall map (OperationResponse operationResponse);
    List<Asset> getAssets (OperationResponse operationResponse);

}
