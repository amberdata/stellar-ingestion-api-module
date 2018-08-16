package io.amberdata.ingestion.stellar.client;

import org.stellar.sdk.responses.operations.OperationResponse;

import com.google.gson.annotations.SerializedName;

public class PreAuthOperationResponse extends OperationResponse {
    @SerializedName("id")
    protected Long   id;
    @SerializedName("source_account")
    protected String sourceAccount;
    @SerializedName("paging_token")
    protected String pagingToken;
    @SerializedName("created_at")
    protected String createdAt;
    @SerializedName("transaction_hash")
    protected String transactionHash;
    @SerializedName("type")
    protected String type;
    @SerializedName("signer_key")
    protected String signerKey;
    @SerializedName("signer_weight")
    protected String signerWeight;

    public PreAuthOperationResponse (
        Long id,
        String sourceAccount,
        String pagingToken,
        String createdAt,
        String transactionHash,
        String type,
        String signerKey,
        String signerWeight
    ) {
        this.id = id;
        this.sourceAccount = sourceAccount;
        this.pagingToken = pagingToken;
        this.createdAt = createdAt;
        this.transactionHash = transactionHash;
        this.type = type;
        this.signerKey = signerKey;
        this.signerWeight = signerWeight;
    }

    @Override
    public Long getId () {
        return this.id;
    }

    public String getPreAuthSourceAccount () {
        return this.sourceAccount;
    }

    @Override
    public String getPagingToken () {
        return this.pagingToken;
    }

    @Override
    public String getCreatedAt () {
        return this.createdAt;
    }

    @Override
    public String getTransactionHash () {
        return this.transactionHash;
    }

    @Override
    public String getType () {
        return this.type;
    }

    public String getSignerKey () {
        return this.signerKey;
    }

    public String getSignerWeight () {
        return this.signerWeight;
    }
}
