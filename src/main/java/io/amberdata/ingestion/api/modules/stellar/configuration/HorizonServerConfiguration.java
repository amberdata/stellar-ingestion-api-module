package io.amberdata.ingestion.api.modules.stellar.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.sdk.Server;

@Configuration
public class HorizonServerConfiguration {

    @Value("${stellar.horizon.server}")
    private String horizonServerUrl;

    @Bean
    public Server horizonServer () {
        return new Server("https://horizon-testnet.stellar.org");
    }
}
