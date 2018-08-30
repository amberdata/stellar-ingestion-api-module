package io.amberdata.ingestion.stellar.mapper;

import io.amberdata.ingestion.core.client.BlockchainEntityWithState;
import io.amberdata.ingestion.core.state.entities.ResourceState;
import io.amberdata.ingestion.stellar.mapper.operations.OperationMapperManager;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.AssetResponse;
import org.stellar.sdk.responses.LedgerResponse;
import org.stellar.sdk.responses.TransactionResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.ingestion.domain.Address;
import io.amberdata.ingestion.domain.Asset;
import io.amberdata.ingestion.domain.Block;
import io.amberdata.ingestion.domain.FunctionCall;
import io.amberdata.ingestion.domain.Transaction;

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
            .meta(blockOptionalProperties(ledgerResponse))
            .build();

        return BlockchainEntityWithState.from(
            block,
            ResourceState.from(Block.class.getSimpleName(), ledgerResponse.getPagingToken())
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
        List<FunctionCall> functionCalls = this.map(operationResponses, transactionResponse.getLedger());
        Set<String> tos = new HashSet<>();
        for (FunctionCall functionCall : functionCalls) {
            String to = functionCall.getTo();
            if (to != null && !to.equals("null") && !to.isEmpty()) {
                tos.add(to);
            }
        }
        String to = null;
        if (tos.size() == 1) {
            to = tos.iterator().next();
        } else if (tos.size() >= 2) {
            to = "_";
        }
        Transaction transaction = new Transaction.Builder()
            .hash(transactionResponse.getHash())
            .nonce(BigInteger.valueOf(transactionResponse.getSourceAccountSequence()))
            .blockNumber(BigInteger.valueOf(transactionResponse.getLedger()))
            .from(transactionResponse.getSourceAccount().getAccountId())
            .to(to)
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
            ResourceState.from(Transaction.class.getSimpleName(), transactionResponse.getPagingToken())
        );
    }

    public List<FunctionCall> map (List<OperationResponse> operationResponses, Long ledger) {
        return IntStream
            .range(0, operationResponses.size())
            .mapToObj(index -> this.operationMapperManager.map(operationResponses.get(index), ledger, index))
            .collect(Collectors.toList());
    }

    public List<Asset> mapAssets (List<OperationResponse> operationResponses, Long ledger) {
        List<Asset> allAssets = new ArrayList<>();
        for (int i = 0; i < operationResponses.size(); i++) {
            OperationResponse operationResponse = operationResponses.get(i);
            List<Asset> assets = this.operationMapperManager.mapAssets(operationResponse);
            for (Asset asset : assets) {
                asset.setTimestamp(Instant.parse(operationResponse.getCreatedAt()).toEpochMilli());
                asset.setTransactionHash(operationResponse.getTransactionHash());
                asset.setFunctionCallHash(
                    String.valueOf(ledger) + "_" +
                        operationResponse.getTransactionHash() + "_" +
                        String.valueOf(i)
                );
            }
            allAssets.addAll(assets);
        }
        return allAssets;
    }

    public BlockchainEntityWithState<Address> map (AccountResponse accountResponse,
                                                   String pagingToken,
                                                   Long timestamp) {
        Address address = new Address.Builder()
            .hash(accountResponse.getKeypair().getAccountId())
            .timestamp(timestamp)
            .meta(addressOptionalProperties(accountResponse))
            .build();

        return BlockchainEntityWithState.from(
            address,
            ResourceState.from(Address.class.getSimpleName(), pagingToken)
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

        optionalProperties.put("public_key", signer.getKey());
        optionalProperties.put("weight", String.valueOf(signer.getWeight()));

        return optionalProperties;
    }
}
