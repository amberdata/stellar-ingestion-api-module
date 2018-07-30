package io.amberdata.ingestion.api.modules.stellar.mapper.operations;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.stellar.sdk.responses.operations.AccountMergeOperationResponse;
import org.stellar.sdk.responses.operations.AllowTrustOperationResponse;
import org.stellar.sdk.responses.operations.ChangeTrustOperationResponse;
import org.stellar.sdk.responses.operations.CreateAccountOperationResponse;
import org.stellar.sdk.responses.operations.CreatePassiveOfferOperationResponse;
import org.stellar.sdk.responses.operations.InflationOperationResponse;
import org.stellar.sdk.responses.operations.ManageDataOperationResponse;
import org.stellar.sdk.responses.operations.ManageOfferOperationResponse;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PathPaymentOperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;
import org.stellar.sdk.responses.operations.SetOptionsOperationResponse;

import io.amberdata.domain.operations.Operation;
import io.amberdata.ingestion.api.modules.stellar.mapper.AssetMapper;

@Component
public class OperationMapperManager {

    private Map<Class<? extends OperationResponse>, OperationMapper> responsesMap;

    @Autowired
    public OperationMapperManager (AssetMapper assetMapper) {
        responsesMap = new HashMap<>();
        responsesMap.put(CreateAccountOperationResponse.class, new CreateAccountOperationMapper());
        responsesMap.put(PaymentOperationResponse.class, new PaymentOperationMapper(assetMapper));
        responsesMap.put(PathPaymentOperationResponse.class, new PathPaymentOperationMapper(assetMapper));
        responsesMap.put(ManageOfferOperationResponse.class, new ManageOfferOperationMapper(assetMapper));
        responsesMap.put(CreatePassiveOfferOperationResponse.class, new CreatePassiveOfferOperationMapper(assetMapper));
        responsesMap.put(SetOptionsOperationResponse.class, new SetOptionsOperationMapper());
        responsesMap.put(ChangeTrustOperationResponse.class, new ChangeTrustOperationMapper(assetMapper));
        responsesMap.put(AllowTrustOperationResponse.class, new AllowTrustOperationMapper(assetMapper));
        responsesMap.put(AccountMergeOperationResponse.class, new AccountMergeOperationMapper());
        responsesMap.put(InflationOperationResponse.class, new InflationOperationMapper());
        responsesMap.put(ManageDataOperationResponse.class, new ManageDataOperationMapper());
    }

    public Operation map (OperationResponse operationResponse) {
        OperationMapper operationMapper = responsesMap.get(operationResponse.getClass());
        return operationMapper.map(operationResponse);
    }
}