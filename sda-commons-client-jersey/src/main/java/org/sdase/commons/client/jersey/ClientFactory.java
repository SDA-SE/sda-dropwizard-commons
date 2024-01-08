package org.sdase.commons.client.jersey;

import io.dropwizard.core.setup.Environment;
import io.opentelemetry.api.OpenTelemetry;
import org.sdase.commons.client.jersey.builder.ExternalClientBuilder;
import org.sdase.commons.client.jersey.builder.PlatformClientBuilder;

/**
 * A {@code ClientFactory} creates Http clients to access services within the SDA Platform or
 * external services.
 */
public class ClientFactory {

  private final Environment environment;
  private final String consumerToken;
  private final OpenTelemetry openTelemetry;

  ClientFactory(Environment environment, String consumerToken, OpenTelemetry openTelemetry) {
    this.environment = environment;
    this.consumerToken = consumerToken;
    this.openTelemetry = openTelemetry;
  }

  /**
   * Starts creation of a client that calls APIs within the SDA SE Platform. This clients
   * automatically send a {@code Trace-Token} from the incoming request or a new {@code Trace-Token}
   * to the API resources and can optionally send a {@code Consumer-Token} or pass through the
   * {@code Authorization} header from the incoming request.
   *
   * <p>The client is using gzip compression.
   *
   * @return a builder to configure the client
   */
  public PlatformClientBuilder platformClient() {
    return platformClient(new HttpClientConfiguration());
  }

  /**
   * Starts creation of a client that calls APIs within the SDA SE Platform. This clients
   * automatically send a {@code Trace-Token} from the incoming request or a new {@code Trace-Token}
   * to the API resources and can optionally send a {@code Consumer-Token} or pass through the
   * {@code Authorization} header from the incoming request.
   *
   * @param httpClientConfiguration Allows to pass additional configuration for the http client.
   * @return a builder to configure the client
   */
  public PlatformClientBuilder platformClient(HttpClientConfiguration httpClientConfiguration) {
    return new PlatformClientBuilder(
        environment, httpClientConfiguration, openTelemetry, consumerToken);
  }

  /**
   * Starts creation of a client that calls APIs outside of the SDA SE Platform. This clients does
   * no header magic.
   *
   * <p>The client is using gzip compression.
   *
   * @return a builder to configure the client
   */
  public ExternalClientBuilder externalClient() {
    return externalClient(new HttpClientConfiguration());
  }

  /**
   * Starts creation of a client that calls APIs outside of the SDA SE Platform. This clients does
   * no header magic.
   *
   * @param httpClientConfiguration Allows to pass additional configuration for the http client.
   * @return a builder to configure the client
   */
  public ExternalClientBuilder externalClient(HttpClientConfiguration httpClientConfiguration) {
    return new ExternalClientBuilder(environment, httpClientConfiguration, openTelemetry);
  }
}
