package org.sdase.commons.server.healthcheck.helper;

import java.net.HttpURLConnection;
import org.sdase.commons.server.healthcheck.ExternalHealthCheck;

/**
 * Default health check for checking if an external URL returns status code 2xx when invoking a
 * simple HTTP GET
 *
 * @deprecated This class will be replaced by {@link
 *     org.sdase.commons.server.dropwizard.healthcheck.helper.ExternalServiceHealthCheck} when
 *     removing the module {@code sda-commons-server-healthcheck}. To prepare for the upcoming
 *     breaking change, update all references to {@link
 *     org.sdase.commons.server.dropwizard.healthcheck.helper.ExternalServiceHealthCheck} and remove
 *     direct dependencies to {@code sda-commons-server-healthcheck}.
 */
@Deprecated(forRemoval = true)
@ExternalHealthCheck
public class ExternalServiceHealthCheck
    extends org.sdase.commons.server.dropwizard.healthcheck.helper.ExternalServiceHealthCheck {

  /**
   * @param url the URL that is requested using {@code GET}
   * @param timeoutMillis the timeout for establishing the connection and waiting for data
   */
  public ExternalServiceHealthCheck(String url, int timeoutMillis) {
    super(url, timeoutMillis);
  }

  /**
   * @param requestMethod the method to use for the check request, {@code GET} and {@code HEAD} are
   *     allowed values
   * @param url the URL that is requested using {@code GET}
   * @param timeoutMillis the timeout for establishing the connection and waiting for data
   */
  public ExternalServiceHealthCheck(String requestMethod, String url, int timeoutMillis) {
    super(requestMethod, url, timeoutMillis);
  }

  /**
   * @param url the URL that is requested using {@code GET}
   * @param timeoutMillis the timeout for establishing the connection and waiting for data
   * @param openConnection provider of the {@link HttpURLConnection}
   */
  @SuppressWarnings("WeakerAccess")
  public ExternalServiceHealthCheck(
      String url, int timeoutMillis, OpenConnectionFunction openConnection) {
    super(url, timeoutMillis, openConnection);
  }

  /**
   * @param requestMethod the method to use for the check request, {@code GET} and {@code HEAD} are
   *     allowed values
   * @param url the URL that is requested using {@code GET}
   * @param timeoutMillis the timeout for establishing the connection and waiting for data
   * @param openConnection provider of the {@link HttpURLConnection}
   */
  public ExternalServiceHealthCheck(
      String requestMethod, String url, int timeoutMillis, OpenConnectionFunction openConnection) {
    super(requestMethod, url, timeoutMillis, openConnection);
  }
}
