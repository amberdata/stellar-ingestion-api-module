package io.amberdata.ingestion.api.modules.stellar.mapper;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.LedgerResponse;
import org.stellar.sdk.responses.TransactionResponse;
import org.stellar.sdk.responses.operations.OperationResponse;

import io.amberdata.domain.Address;
import io.amberdata.domain.Block;
import io.amberdata.domain.Transaction;
import io.amberdata.domain.operations.Operation;

@Component
public class ModelMapper {
    private final String blockChainId;

    public ModelMapper (@Value("${ingestion.api.blockchainId}") String blockChainId) {
        this.blockChainId = blockChainId;
    }

    public Block map (LedgerResponse ledgerResponse) {
        return new Block.Builder()
            .blockchainId(blockChainId)
            .number(BigInteger.valueOf(ledgerResponse.getSequence()))
            .hash(ledgerResponse.getHash())
            .parentHash(ledgerResponse.getPrevHash())
            //.gasUsed(new BigInteger(ledgerResponse.getFeePool())) causes NumberFormatException because of decimal there
            .numTransactions(ledgerResponse.getTransactionCount())
            .timestamp(Instant.parse(ledgerResponse.getClosedAt()).toEpochMilli())
            .optionalProperties(blockOptionalProperties(ledgerResponse))
            .build();
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

    public Transaction map (TransactionResponse transactionResponse) {
        return new Transaction.Builder()
            .blockchainId(blockChainId)
            .hash(transactionResponse.getHash())
            .nonce(BigInteger.valueOf(transactionResponse.getSourceAccountSequence()))
            .blockNumber(BigInteger.valueOf(transactionResponse.getLedger()))
            .from(transactionResponse.getSourceAccount().getAccountId())
            //.gas(transactionResponse.) which property if max_fee doesn't exist????
            .gasUsed(BigInteger.valueOf(transactionResponse.getFeePaid()))
            .numLogs(transactionResponse.getOperationCount())
            .timestamp(Instant.parse(transactionResponse.getCreatedAt()).toEpochMilli())
            .build();
    }

    public Operation map (OperationResponse operationResponse) {
        operationResponse.
    }

    public Address map (AccountResponse accountResponse) {
        return new Address.Builder()
            .hash(accountResponse.getKeypair().getAccountId())
            // need timestamp here
            .optionalProperties(addressOptionalProperties(accountResponse))
            .build();
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
        optionalProperties.put("asset_code", balance.getAssetCode());
        optionalProperties.put("asset_issuer", balance.getAssetIssuer().getAccountId());

        return optionalProperties;
    }

    private Map<String, Object> signerProperty (AccountResponse.Signer signer) {
        Map<String, Object> optionalProperties = new HashMap<>();

        optionalProperties.put("public_key", signer.getAccountId());
        optionalProperties.put("weight", String.valueOf(signer.getWeight()));

        return optionalProperties;
    }
}
