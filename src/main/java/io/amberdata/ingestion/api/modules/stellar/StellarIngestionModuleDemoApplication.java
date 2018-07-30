package io.amberdata.ingestion.api.modules.stellar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;

@SpringBootApplication(exclude = {GsonAutoConfiguration.class})
public class StellarIngestionModuleDemoApplication {
    public static void main (String[] args) throws InterruptedException {
        SpringApplication.run(StellarIngestionModuleDemoApplication.class, args);

        Thread.sleep(60_000);
    }
}