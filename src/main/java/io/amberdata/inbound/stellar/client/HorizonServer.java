package io.amberdata.inbound.stellar.client;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.stellar.sdk.Server;
import org.stellar.sdk.requests.ClientIdentificationInterceptor;
import org.stellar.sdk.requests.ErrorResponse;
import org.stellar.sdk.responses.AccountResponse;

import shadow.okhttp3.Interceptor;
import shadow.okhttp3.OkHttpClient;
import shadow.okhttp3.Request;
import shadow.okhttp3.Response;

@Component
public class HorizonServer {

  private static final Logger LOG = LoggerFactory.getLogger(HorizonServer.class);

  private final Server horizonServer;
  private final String serverUrl;

  private Set<String> missingAddresses;
  private Set<String> knownAddresses;

  private LruCache<String, AccountResponse> addressCache;
  private long cacheCreationTime;
  private static final int MAX_ADDRESS_CACHE_SIZE = 1024;
  private static final int MAX_ADDRESS_CACHE_AGE_MILLIS = 3600000;

  public static final int HORIZON_PER_REQUEST_LIMIT = 200;

  // Helper to profile every URL hit by Stellar Java SDK to check how performance can be improved.
  private class LogInterceptor implements Interceptor {
    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
      final Request req = chain.request();
      final long start = System.currentTimeMillis();
      final Response resp = chain.proceed(chain.request());
      LOG.info("request url: {} method: {} duration: {}ms",
          req.url().toString(), req.method(), (System.currentTimeMillis() - start));
      return resp;
    }
  }

  /**
   * Default constructor.
   *
   * @param serverUrl the url of the server
   */
  public HorizonServer(@Value("${stellar.horizon.server}") String serverUrl) {
    LOG.info("Horizon server URL {}", serverUrl);

    this.serverUrl = serverUrl;
    this.missingAddresses = new HashSet<>();
    this.knownAddresses = new HashSet<>();
    this.addressCache = new LruCache<>(MAX_ADDRESS_CACHE_SIZE);
    this.cacheCreationTime = System.currentTimeMillis();

    // Stolen from the regular constructor for Server, but increasing
    // |readTimeout| to 90 and 120 seconds respectively since our
    // Horizon/PostgreSQL setup can be pokey
    final OkHttpClient httpClient = new OkHttpClient.Builder()
        .addInterceptor(new ClientIdentificationInterceptor())
        //.addInterceptor(new LogInterceptor())
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build();

    final OkHttpClient submitHttpClient = new OkHttpClient.Builder()
        .addInterceptor(new ClientIdentificationInterceptor())
        //.addInterceptor(new LogInterceptor())
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build();

    this.horizonServer = new Server(serverUrl, httpClient, submitHttpClient);
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

  /**
   * Caching wrapper around fetching details from Horizon about a given Stellar
   * account. The cache is blown away every 2 hours.
   * TODO: Blow things away on a key age basis rather than all at once.
   *
   * @param accountId The alphanumeric account ID to fetch details for.
   *
   * @return the url of the server.
   */
  public AccountResponse fetchAccountDetails(String accountId) {
    final long now = System.currentTimeMillis();
    if (now - this.cacheCreationTime > MAX_ADDRESS_CACHE_AGE_MILLIS) {
      // Intentionally do not clear this.knownAddresses.
      this.missingAddresses = new HashSet<>();
      this.addressCache = new LruCache<>(MAX_ADDRESS_CACHE_SIZE);
      this.cacheCreationTime = now;
    }

    // We already requested this address and it is not found. Skip the request.
    if (missingAddresses.contains(accountId)) {
      return null;
    } else if (!knownAddresses.contains(accountId)) {
      knownAddresses.add(accountId);
      Metrics.count("account.unique", 1);
    } else if (addressCache.containsKey(accountId)) {
      Metrics.count("account.cache.hit", 1);
      return addressCache.get(accountId);
    }

    Metrics.count("account.cache.miss", 1);
    try {
      final AccountResponse resp = this.horizonServer()
          .accounts()
          .account(accountId);

      addressCache.put(accountId, resp);
      return resp;
    } catch (Exception e) {
      if (e instanceof ErrorResponse) {
        ErrorResponse er = (ErrorResponse)e;
        if (er.getCode() == 404) {
          LOG.info("Ignoring not found account ID: {}", accountId);
          missingAddresses.add(accountId);
          Metrics.count("account.errors.not_found", 1);
        } else {
          LOG.error("Unable to get details for account ID ({}): {}: {}",
              er.getCode(), accountId, er.getBody());
          Metrics.count("account.errors.other_http", 1);
        }
      } else {
        LOG.error("Unable to get details for account ID: {}", accountId, e);
        Metrics.count("account.errors.other", 1);
      }
      return null;
    }
  }
}
