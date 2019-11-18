package io.amberdata.inbound.stellar.client;

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
  private final String serverUrl;

  /**
   * Default constructor.
   *
   * @param serverUrl the url of the server
   */
  public HorizonServer(@Value("${stellar.horizon.server}") String serverUrl) {
    LOG.info("Horizon server URL {}", serverUrl);

    this.serverUrl = serverUrl;
    this.horizonServer = new Server(serverUrl);
  }

  /**
   * Returns the url of the server.
   *
   * @return the url of the server.
   */
  public String getServerUrl() {
    return this.serverUrl;
  }

  /**
   * Returns the Horizon server.
   *
   * @return the Horizon server.
   */
  public Server horizonServer() {
    return this.horizonServer;
  }

  /**
   * Tests the connection to the server.
   */
  public void testConnection() {
    try {
      this.horizonServer.root().getCurrentProtocolVersion();
    } catch (IOException ioe) {
      throw new ServerConnectionException("Cannot resolve connection to Horizon server", ioe);
    }
  }

  public static class ServerConnectionException extends RuntimeException {
    public ServerConnectionException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  public static class IncorrectRequestException extends RuntimeException {
    public IncorrectRequestException(String message) {
      super(message);
    }

    public IncorrectRequestException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  public static class StellarException extends RuntimeException {
    public StellarException(String message) {
      super(message);
    }

    public StellarException(String message, Throwable cause) {
      super(message, cause);
    }
  }

}
