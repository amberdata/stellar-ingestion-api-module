package io.amberdata.ingestion.api.modules.conf;

import java.util.Arrays;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import org.springframework.stereotype.Component;
import org.stellar.sdk.Server;
import org.stellar.sdk.requests.EventListener;
import org.stellar.sdk.requests.RequestBuilder;
import org.stellar.sdk.responses.LedgerResponse;

@Component
public class DemoApplicationRunner implements ApplicationRunner {
    @Override
    public void run (ApplicationArguments args) throws Exception {
//        Server server = new Server("https://horizon-testnet.stellar.org");
        Server server = new Server("http://localhost:8000");

        server
            .ledgers()
            .cursor("now")
            .stream(ledger -> System.err.println(ledger.getClosedAt() + ": " + ledger.getSequence()));

        server
            .transactions()
            .cursor("now")
            .stream(tx -> System.out.println(tx.getCreatedAt() + ": " + tx.getLedger() + " " + tx.getHash()));

        server
            .accounts()
            .cursor("now")
            .stream(acc -> System.out.println("acc: " + acc.getKeypair().getAccountId() + " " + Arrays.toString(acc.getKeypair().getSecretSeed())));

        server
            .operations()
            .cursor("now")
            .stream(op -> System.out.println("Op " + op.getType() + " from " + op.getSourceAccount().getAccountId() + " hash " + op.getTransactionHash()));

        System.out.println("weeeee-eeeee-eeeee-eeeee");


        Thread.sleep(100000);
    }
}
