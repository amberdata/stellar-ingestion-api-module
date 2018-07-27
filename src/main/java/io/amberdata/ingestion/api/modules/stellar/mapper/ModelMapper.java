package io.amberdata.ingestion.api.modules.stellar.mapper;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.LedgerResponse;
import org.stellar.sdk.responses.TransactionResponse;

import io.amberdata.domain.Address;
import io.amberdata.domain.Block;
import io.amberdata.domain.Transaction;

@Component
public class ModelMapper {
    private final String blockChainId;

    public ModelMapper (@Value("ingestion.api.blockchainId") String blockChainId) {
        this.blockChainId = blockChainId;
    }

    public Block map (LedgerResponse ledgerResponse) {
        Map<String, Object> optionalProperties = new HashMap<>();
        optionalProperties.put("operation_count", ledgerResponse.getOperationCount());
        optionalProperties.put("total_coins", ledgerResponse.getTotalCoins());
        optionalProperties.put("base_fee_in_stroops", ledgerResponse.getBaseFeeInStroops());
        optionalProperties.put("base_reserve_in_stroops", ledgerResponse.getBaseReserveInStroops());
        optionalProperties.put("max_tx_set_size", ledgerResponse.getMaxTxSetSize());

        return new Block.Builder()
            .blockchainId(blockChainId)
            .number(BigInteger.valueOf(ledgerResponse.getSequence()))
            .hash(ledgerResponse.getHash())
            .parentHash(ledgerResponse.getPrevHash())
            .gasUsed(new BigInteger(ledgerResponse.getFeePool()))
            .numTransactions(ledgerResponse.getTransactionCount())
            .timestamp(Long.valueOf(ledgerResponse.getClosedAt()))
            .optionalProperties(optionalProperties)
            .build();
    }

    public Transaction map (TransactionResponse transactionResponse) {
        Map<String, Object> optionalProperties = new HashMap<>();
        return new Transaction.Builder()
            .blockchainId(blockChainId)
            .hash(transactionResponse.getHash())
            .nonce(BigInteger.valueOf(transactionResponse.getSourceAccountSequence()))
            .blockNumber(BigInteger.valueOf(transactionResponse.getLedger()))
            .from(transactionResponse.getSourceAccount().getAccountId())
            //.gas(transactionResponse.) which property if max_fee doesn't exist????
            .gasUsed(BigInteger.valueOf(transactionResponse.getFeePaid()))
            .numLogs(Integer.valueOf(transactionResponse.getCreatedAt()))
            .optionalProperties(optionalProperties)
            .build();
    }

    public Address map (AccountResponse accountResponse) {
        Map<String, Object>       optionalProperties = new HashMap<>();
        List<Map<String, String>> balances           = new ArrayList<>();
        List<Map<String, String>> signers            = new ArrayList<>();

        for (AccountResponse.Balance balance : accountResponse.getBalances()) {
            Map<String, String> data = new HashMap<>();
            data.put("balance", balance.getBalance());
            data.put("limit", balance.getLimit());
            data.put("asset_type", balance.getAssetType());
            data.put("asset_code", balance.getAssetCode());
            data.put("asset_issuer", balance.getAssetIssuer().getAccountId());
            balances.add(data);
        }

        for (AccountResponse.Signer signer : accountResponse.getSigners()) {
            Map<String, String> data = new HashMap<>();
            data.put("public_key", signer.getAccountId());
            data.put("weight", String.valueOf(signer.getWeight()));
            signers.add(data);
        }

        optionalProperties.put("sequence", accountResponse.getSequenceNumber());
        optionalProperties.put("subentry_count", accountResponse.getSubentryCount());
        optionalProperties.put("threshold_low", accountResponse.getThresholds().getLowThreshold());
        optionalProperties.put("threshold_med", accountResponse.getThresholds().getMedThreshold());
        optionalProperties.put("threshold_high", accountResponse.getThresholds().getHighThreshold());
        optionalProperties.put("flag_auth_required", accountResponse.getFlags().getAuthRequired());
        optionalProperties.put("flag_auth_revocable", accountResponse.getFlags().getAuthRevocable());
        optionalProperties.put("balances", balances);
        optionalProperties.put("signers", signers);

        return new Address.Builder()
            .hash(accountResponse.getKeypair().getAccountId())
            .optionalProperties(optionalProperties)
            .build();
    }
}
