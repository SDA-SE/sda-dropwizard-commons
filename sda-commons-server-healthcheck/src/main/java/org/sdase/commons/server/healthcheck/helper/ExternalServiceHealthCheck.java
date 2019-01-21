package org.sdase.commons.server.healthcheck.helper;

import com.codahale.metrics.health.HealthCheck;
import org.sdase.commons.server.healthcheck.ExternalHealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static javax.servlet.http.HttpServletResponse.SC_OK;

/**
 * Default health check for checking if an external URL returns status code 200
 * when invoking a simple HTTP GET
 */
@ExternalHealthCheck
public class ExternalServiceHealthCheck extends HealthCheck {

   private static final Logger LOGGER = LoggerFactory.getLogger(ExternalServiceHealthCheck.class);

   private final String url;
   private final int timeout;
   private final OpenConnectionFunction openConnection;

   public ExternalServiceHealthCheck(String url, int timeout) {
      super();
      this.url = url;
      this.timeout = timeout;
      this.openConnection = this::openHttpURLConnection;
   }

   @SuppressWarnings("WeakerAccess")
   public ExternalServiceHealthCheck(String url, int timeout, OpenConnectionFunction openConnection) {
      super();
      this.url = url;
      this.timeout = timeout;
      this.openConnection = openConnection;
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
            connection.setRequestMethod("GET");
            statusCode = connection.getResponseCode();
            if (SC_OK == statusCode) {
               LOGGER.info("Endpoint is available (URL: {})", this.url);
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

   private HttpURLConnection openHttpURLConnection(String url) throws IOException {
      return (HttpURLConnection) new URL(url).openConnection();
   }
}
