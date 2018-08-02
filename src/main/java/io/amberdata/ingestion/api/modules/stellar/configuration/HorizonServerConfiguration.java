package io.amberdata.ingestion.api.modules.stellar.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.sdk.Server;

@Configuration
public class HorizonServerConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(HorizonServerConfiguration.class);

    @Bean
    public Server horizonServer (@Value("${stellar.horizon.server}") String serverUrl) {
        LOG.info("Horizon server URL {}", serverUrl);

        return new Server(serverUrl);
    }
}
