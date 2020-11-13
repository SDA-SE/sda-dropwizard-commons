package org.sdase.commons.client.jersey;

/**
 * A configuration class to be used for API clients that are created as Proxy from annotated
 * interfaces.
 */
public class ApiHttpClientConfiguration extends HttpClientConfiguration {
  private String apiBaseUrl;

  /** @return the base URL of the implemented API, e.g. {@code http://the-service:8080/api} */
  public String getApiBaseUrl() {
    return apiBaseUrl;
  }

  /**
   * @param apiBaseUrl the base URL of the implemented API, e.g. {@code http://the-service:8080/api}
   * @return this instance for fluent configuration
   */
  public ApiHttpClientConfiguration setApiBaseUrl(String apiBaseUrl) {
    this.apiBaseUrl = apiBaseUrl;
    return this;
  }
}
