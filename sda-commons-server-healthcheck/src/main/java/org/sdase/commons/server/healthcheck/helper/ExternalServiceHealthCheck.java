package org.sdase.commons.server.healthcheck.helper;

import com.codahale.metrics.health.HealthCheck;
import org.eclipse.jetty.http.HttpStatus;
import org.sdase.commons.server.healthcheck.ExternalHealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Default health check for checking if an external URL returns status code 2xx
 * when invoking a simple HTTP GET
 */
@ExternalHealthCheck
public class ExternalServiceHealthCheck extends HealthCheck {

   private static final Logger LOGGER = LoggerFactory.getLogger(ExternalServiceHealthCheck.class);

   private final String requestMethod;
   private final String url;
   private final int timeout;
   private final OpenConnectionFunction openConnection;

   /**
    * @param url the URL that is requested using {@code GET}
    * @param timeoutMillis the timeout for establishing the connection and waiting for data
    */
   public ExternalServiceHealthCheck(String url, int timeoutMillis) {
      this("GET", url, timeoutMillis, ExternalServiceHealthCheck::openHttpURLConnection);
   }

   /**
    * @param requestMethod the method to use for the check request, {@code GET} and {@code HEAD} are allowed values
    * @param url the URL that is requested using {@code GET}
    * @param timeoutMillis the timeout for establishing the connection and waiting for data
    */
   public ExternalServiceHealthCheck(String requestMethod, String url, int timeoutMillis) {
      this(requestMethod, url, timeoutMillis, ExternalServiceHealthCheck::openHttpURLConnection);
   }

   /**
    * @param url the URL that is requested using {@code GET}
    * @param timeoutMillis the timeout for establishing the connection and waiting for data
    * @param openConnection provider of the {@link HttpURLConnection}
    */
   @SuppressWarnings("WeakerAccess")
   public ExternalServiceHealthCheck(String url, int timeoutMillis, OpenConnectionFunction openConnection) {
      this("GET", url, timeoutMillis, openConnection);
   }

   /**
    * @param requestMethod the method to use for the check request, {@code GET} and {@code HEAD} are allowed values
    * @param url the URL that is requested using {@code GET}
    * @param timeoutMillis the timeout for establishing the connection and waiting for data
    * @param openConnection provider of the {@link HttpURLConnection}
    */
   public ExternalServiceHealthCheck(String requestMethod, String url, int timeoutMillis, OpenConnectionFunction openConnection) {
      super();
      if ("GET".equalsIgnoreCase(requestMethod)) {
         this.requestMethod = "GET";
      }
      else if ("HEAD".equalsIgnoreCase(requestMethod)) {
         this.requestMethod = "HEAD";
      }
      else {
         throw new IllegalArgumentException(
               "Only GET and HEAD are allowed as requestMethod, but requestMethod is" + requestMethod);
      }
      this.url = url;
      this.timeout = timeoutMillis;
      this.openConnection = openConnection;
      LOGGER.info("Creating {} for '{} {}' with timeout of {}ms",
            this.getClass(), this.requestMethod, this.url, this.timeout);
   }

   @Override
   public Result check() throws Exception {
      LOGGER.debug("Check if Endpoint is available (URL: {})", this.url);

      int statusCode;

      try {
         HttpURLConnection connection = openConnection.call(url);

         try (AutoCloseable ignored = connection::disconnect) {
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestMethod(requestMethod);
            statusCode = connection.getResponseCode();
            if (HttpStatus.isSuccess(statusCode)) {
               LOGGER.debug("Endpoint is available (URL: {})", this.url);
               return Result.healthy();
            }
         }
      } catch (IOException exception) {
         LOGGER.warn("IOException (Details in Logs) - Endpoint not available (URL: {})", this.url, exception);
         return Result.unhealthy("IOException (Details in Logs) - Endpoint not available (URL: {})", this.url);
      }

      LOGGER.warn("Endpoint not available (URL: {})", this.url);
      return Result.unhealthy("Endpoint not available (URL: {}, StatusCode: {})", this.url, statusCode);
   }

   private static HttpURLConnection openHttpURLConnection(String url) throws IOException {
      return (HttpURLConnection) new URL(url).openConnection();
   }
}
