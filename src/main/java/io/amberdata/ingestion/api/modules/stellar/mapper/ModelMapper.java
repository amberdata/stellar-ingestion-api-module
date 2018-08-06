package io.amberdata.ingestion.api.modules.stellar.mapper;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.AssetResponse;
import org.stellar.sdk.responses.LedgerResponse;
import org.stellar.sdk.responses.TransactionResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.domain.Address;
import io.amberdata.domain.Asset;
import io.amberdata.domain.Block;
import io.amberdata.domain.FunctionCall;
import io.amberdata.domain.Transaction;
import io.amberdata.ingestion.api.modules.stellar.mapper.operations.OperationMapperManager;
import io.amberdata.ingestion.api.modules.stellar.state.entities.BlockchainEntityWithState;
import io.amberdata.ingestion.api.modules.stellar.state.entities.Resource;
import io.amberdata.ingestion.api.modules.stellar.state.entities.ResourceState;

@Component
public class ModelMapper {
    private static final Logger LOG = LoggerFactory.getLogger(ModelMapper.class);

    private final String blockChainId;

    private final OperationMapperManager operationMapperManager;

    @Autowired
    public ModelMapper (@Value("${ingestion.api.blockchain-id}") String blockChainId,
                        OperationMapperManager operationMapperManager) {
        this.blockChainId = blockChainId;
        this.operationMapperManager = operationMapperManager;
    }

    public BlockchainEntityWithState<Block> map (LedgerResponse ledgerResponse) {
        Block block = new Block.Builder()
            .blockchainId(blockChainId)
            .number(BigInteger.valueOf(ledgerResponse.getSequence()))
            .hash(ledgerResponse.getHash())
            .parentHash(ledgerResponse.getPrevHash())
            //.gasUsed(new BigInteger(ledgerResponse.getFeePool())) causes NumberFormatException because of decimal there
            .numTransactions(ledgerResponse.getTransactionCount())
            .timestamp(Instant.parse(ledgerResponse.getClosedAt()).toEpochMilli())
            .optionalProperties(blockOptionalProperties(ledgerResponse))
            .build();

        return BlockchainEntityWithState.from(
            block,
            ResourceState.from(Resource.LEDGER, ledgerResponse.getPagingToken())
        );
    }

    private Map<String, Object> blockOptionalProperties (LedgerResponse ledgerResponse) {
        Map<String, Object> optionalProperties = new HashMap<>();

        optionalProperties.put("operation_count", ledgerResponse.getOperationCount());
        optionalProperties.put("total_coins", ledgerResponse.getTotalCoins());
        optionalProperties.put("base_fee_in_stroops", ledgerResponse.getBaseFeeInStroops());
        optionalProperties.put("base_reserve_in_stroops", ledgerResponse.getBaseReserveInStroops());
        optionalProperties.put("max_tx_set_size", ledgerResponse.getMaxTxSetSize());

        return optionalProperties;
    }

    public BlockchainEntityWithState<Transaction> map (TransactionResponse transactionResponse,
                                                       List<OperationResponse> operationResponses) {

        Transaction transaction = new Transaction.Builder()
            .blockchainId(blockChainId)
            .hash(transactionResponse.getHash())
            .nonce(BigInteger.valueOf(transactionResponse.getSourceAccountSequence()))
            .blockNumber(BigInteger.valueOf(transactionResponse.getLedger()))
            .from(transactionResponse.getSourceAccount().getAccountId())
            //todo .gas(transactionResponse.) which property if max_fee doesn't exist????
            .gasUsed(BigInteger.valueOf(transactionResponse.getFeePaid()))
            .numLogs(transactionResponse.getOperationCount())
            .timestamp(Instant.parse(transactionResponse.getCreatedAt()).toEpochMilli())
            .functionCalls(this.map(operationResponses))
            .build();

        return BlockchainEntityWithState.from(
            transaction,
            ResourceState.from(Resource.TRANSACTION, transactionResponse.getPagingToken())
        );
    }

    public List<FunctionCall> map (List<OperationResponse> operationResponses) {
        return operationResponses.stream()
            .map(this.operationMapperManager::map)
            .collect(Collectors.toList());
    }

    public List<Asset> mapAssets (List<OperationResponse> operationResponses) {
        return operationResponses.stream()
            .flatMap(operationResponse -> this.operationMapperManager.mapAssets(operationResponse).stream())
            .collect(Collectors.toList());
    }

    public BlockchainEntityWithState<Address> map (AccountResponse accountResponse, String pagingToken) {
        Address address = new Address.Builder()
            .hash(accountResponse.getKeypair().getAccountId())
            //todo need timestamp here
            .optionalProperties(addressOptionalProperties(accountResponse))
            .build();

        return BlockchainEntityWithState.from(
            address,
            ResourceState.from(Resource.ACCOUNT, pagingToken)
        );
    }

    public BlockchainEntityWithState<Asset> map (AssetResponse assetResponse, String pagingToken) {
        Asset asset = new Asset.Builder()
            .type(Asset.AssetType.fromName(assetResponse.getAssetType()))
            .code(assetResponse.getAssetCode())
            .issuerAccount(assetResponse.getAssetIssuer())
            .amount(assetResponse.getAmount())
            .isAuthRequired(assetResponse.getFlags().isAuthRequired())
            .isAuthRevocable(assetResponse.getFlags().isAuthRevocable())
            .build();

        return BlockchainEntityWithState.from(
            asset,
            ResourceState.from(Resource.ASSET, pagingToken)
        );
    }

    private Map<String, Object> addressOptionalProperties (AccountResponse accountResponse) {
        Map<String, Object> optionalProperties = new HashMap<>();

        optionalProperties.put("sequence", accountResponse.getSequenceNumber());
        optionalProperties.put("subentry_count", accountResponse.getSubentryCount());
        optionalProperties.put("threshold_low", accountResponse.getThresholds().getLowThreshold());
        optionalProperties.put("threshold_med", accountResponse.getThresholds().getMedThreshold());
        optionalProperties.put("threshold_high", accountResponse.getThresholds().getHighThreshold());
        optionalProperties.put("flag_auth_required", accountResponse.getFlags().getAuthRequired());
        optionalProperties.put("flag_auth_revocable", accountResponse.getFlags().getAuthRevocable());
        optionalProperties.put("balances", Arrays.stream(accountResponse.getBalances())
            .map(this::balanceProperty)
            .collect(Collectors.toList()));
        optionalProperties.put("signers", Arrays.stream(accountResponse.getSigners())
            .map(this::signerProperty)
            .collect(Collectors.toList()));

        return optionalProperties;
    }

    private Map<String, Object> balanceProperty (AccountResponse.Balance balance) {
        Map<String, Object> optionalProperties = new HashMap<>();

        optionalProperties.put("balance", balance.getBalance());
        optionalProperties.put("limit", balance.getLimit());
        optionalProperties.put("asset_type", balance.getAssetType());
        if (!balance.getAssetType().equals("native")) {
            optionalProperties.put("asset_code", balance.getAssetCode());
            if (balance.getAssetIssuer() == null) {
                LOG.warn("AssetIssuer in mapping balance property is null");
            }
            else {
                optionalProperties.put("asset_issuer", balance.getAssetIssuer().getAccountId());
            }
        }

        return optionalProperties;
    }

    private Map<String, Object> signerProperty (AccountResponse.Signer signer) {
        Map<String, Object> optionalProperties = new HashMap<>();

        optionalProperties.put("public_key", signer.getAccountId());
        optionalProperties.put("weight", String.valueOf(signer.getWeight()));

        return optionalProperties;
    }
}
