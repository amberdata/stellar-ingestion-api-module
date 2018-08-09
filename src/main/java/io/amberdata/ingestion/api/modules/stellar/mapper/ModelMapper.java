package io.amberdata.ingestion.api.modules.stellar.mapper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import io.amberdata.domain.Token;
import io.amberdata.domain.Transaction;
import io.amberdata.ingestion.api.modules.stellar.mapper.operations.OperationMapperManager;
import io.amberdata.ingestion.api.modules.stellar.state.entities.BlockchainEntityWithState;
import io.amberdata.ingestion.api.modules.stellar.state.entities.Resource;
import io.amberdata.ingestion.api.modules.stellar.state.entities.ResourceState;

@Component
public class ModelMapper {
    private static final Logger LOG = LoggerFactory.getLogger(ModelMapper.class);

    private final OperationMapperManager operationMapperManager;

    @Autowired
    public ModelMapper (OperationMapperManager operationMapperManager) {
        this.operationMapperManager = operationMapperManager;
    }

    public BlockchainEntityWithState<Block> map (LedgerResponse ledgerResponse) {
        Block block = new Block.Builder()
            .number(BigInteger.valueOf(ledgerResponse.getSequence()))
            .hash(ledgerResponse.getHash())
            .parentHash(ledgerResponse.getPrevHash())
            .gasUsed(new BigDecimal(ledgerResponse.getFeePool()))
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
        optionalProperties.put("sequence", ledgerResponse.getSequence());

        return optionalProperties;
    }

    public BlockchainEntityWithState<Transaction> map (TransactionResponse transactionResponse,
                                                       List<OperationResponse> operationResponses) {

        Transaction transaction = new Transaction.Builder()
            .hash(transactionResponse.getHash())
            .nonce(BigInteger.valueOf(transactionResponse.getSourceAccountSequence()))
            .blockNumber(BigInteger.valueOf(transactionResponse.getLedger()))
            .from(transactionResponse.getSourceAccount().getAccountId())
            //todo .gas(transactionResponse.) which property if max_fee doesn't exist????
            .gasUsed(BigInteger.valueOf(transactionResponse.getFeePaid()))
            .numLogs(transactionResponse.getOperationCount())
            .timestamp(Instant.parse(transactionResponse.getCreatedAt()).toEpochMilli())
            .functionCalls(this.map(operationResponses, transactionResponse.getLedger()))
            .status("0x1")
            .value(BigDecimal.ZERO)
            .build();

        return BlockchainEntityWithState.from(
            transaction,
            ResourceState.from(Resource.TRANSACTION, transactionResponse.getPagingToken())
        );
    }

    public List<FunctionCall> map (List<OperationResponse> operationResponses, Long ledger) {
        return IntStream
            .range(0, operationResponses.size())
            .mapToObj(index -> this.operationMapperManager.map(operationResponses.get(index), ledger, index))
            .collect(Collectors.toList());
    }

    public List<Asset> mapAssets (List<OperationResponse> operationResponses) {
        return operationResponses.stream()
            .flatMap(operationResponse -> this.operationMapperManager.mapAssets(operationResponse).stream())
            .collect(Collectors.toList());
    }

    public BlockchainEntityWithState<Address> map (AccountResponse accountResponse,
                                                   String pagingToken,
                                                   Long timestamp) {
        Address address = new Address.Builder()
            .hash(accountResponse.getKeypair().getAccountId())
            .timestamp(timestamp)
            .optionalProperties(addressOptionalProperties(accountResponse))
            .build();

        return BlockchainEntityWithState.from(
            address,
            ResourceState.from(Resource.ACCOUNT, pagingToken)
        );
    }

    public BlockchainEntityWithState<Token> map (AssetResponse assetResponse, String pagingToken) {

        Token token = new Token.Builder()
            .address(assetResponse.getAssetIssuer())
            .symbol(assetResponse.getAssetCode())
            .name(assetResponse.getAssetType())
            .decimals(new BigDecimal(assetResponse.getAmount()))
            .optionalProperties(tokenOptionalProperties(assetResponse))
            .build();

        return BlockchainEntityWithState.from(
            token,
            ResourceState.from(Resource.TOKEN, pagingToken)
        );
    }

    private Map<String, Object> tokenOptionalProperties (AssetResponse assetResponse) {
        Map<String, Object> optionalProperties = new HashMap<>();

        optionalProperties.put("num_accounts", assetResponse.getNumAccounts());
        optionalProperties.put("flag_auth_required", assetResponse.getFlags().isAuthRequired());
        optionalProperties.put("flag_auth_revocable", assetResponse.getFlags().isAuthRevocable());

        return optionalProperties;
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
