package io.amberdata.ingestion.api.modules.stellar.client;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.stellar.sdk.Server;

@Component
public class HorizonServer {
    private static final Logger LOG = LoggerFactory.getLogger(HorizonServer.class);

    private final Server horizonServer;

    public HorizonServer (@Value("${stellar.horizon.server}") String serverUrl) {
        LOG.info("Horizon server URL {}", serverUrl);

        this.horizonServer = new Server(serverUrl);
    }

    public Server horizonServer () {
        return horizonServer;
    }

    public void testConnection () {
        try {
            horizonServer.root().getProtocolVersion();
        }
        catch (IOException e) {
            throw new ServerConnectionException("Cannot resolve connection to Horizon server", e);
        }
    }

    public static class ServerConnectionException extends RuntimeException {
        public ServerConnectionException (String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class IncorrectRequestException extends RuntimeException {
        public IncorrectRequestException (String message, Throwable cause) {
            super(message, cause);
        }
    }
}