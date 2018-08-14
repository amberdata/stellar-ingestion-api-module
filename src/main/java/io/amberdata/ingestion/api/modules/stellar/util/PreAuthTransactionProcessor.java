package io.amberdata.ingestion.api.modules.stellar.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.stellar.sdk.responses.operations.OperationResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.amberdata.domain.PreAuthOperationResponse;
import io.amberdata.ingestion.api.modules.stellar.client.HorizonServer;

@Component
public class PreAuthTransactionProcessor {

    private final HorizonServer server;

    @Autowired
    public PreAuthTransactionProcessor (HorizonServer server) {
        this.server = server;
    }

    public List<OperationResponse> fetchOperations (String transaction) {
        OkHttpClient okHttpClient = server.horizonServer().getHttpClient();
        String   url          = server.getServerUrl() + "/transactions/" + transaction + "/operations";
        Request  request      = (new Request.Builder()).get().url(url).build();
        try {
            Response                response           = okHttpClient.newCall(request).execute();
            String                  body               = response.body().string();
            JsonElement             jelement           = new JsonParser().parse(body);
            JsonObject              jobject            = jelement.getAsJsonObject();
            JsonArray               jsonArray          = jobject.getAsJsonObject("_embedded").getAsJsonArray("records");
            List<OperationResponse> operationResponses = new ArrayList<>();
            for (JsonElement jsonElement : jsonArray) {
                JsonObject elements = jsonElement.getAsJsonObject();
                OperationResponse operationResponse = new PreAuthOperationResponse(
                    elements.get("id").getAsLong(),
                    elements.get("source_account").getAsString(),
                    elements.get("paging_token").getAsString(),
                    elements.get("created_at").getAsString(),
                    elements.get("transaction_hash").getAsString(),
                    elements.get("type").getAsString(),
                    elements.get("signer_key").getAsString(),
                    elements.get("signer_weight").getAsString()
                );
                operationResponses.add(operationResponse);
            }
            return operationResponses;
        }
        catch (IOException e) {
            return Collections.emptyList();
        }
    }
}
