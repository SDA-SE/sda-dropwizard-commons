package org.sdase.commons.client.jersey;

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.setup.Environment;
import io.opentracing.Tracer;
import org.sdase.commons.client.jersey.builder.ExternalClientBuilder;
import org.sdase.commons.client.jersey.builder.PlatformClientBuilder;

/**
 * A {@code ClientFactory} creates Http clients to access services within the SDA Platform or
 * external services.
 */
public class ClientFactory {

  private final Environment environment;
  private final String consumerToken;
  private final Tracer tracer;

  ClientFactory(Environment environment, String consumerToken, Tracer tracer) {
    this.environment = environment;
    this.consumerToken = consumerToken;
    this.tracer = tracer;
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
        createClientBuilder(httpClientConfiguration), tracer, consumerToken);
  }

  /**
   * Starts creation of a client that calls APIs within the SDA SE Platform. This clients
   * automatically send a {@code Trace-Token} from the incoming request or a new {@code Trace-Token}
   * to the API resources and can optionally send a {@code Consumer-Token} or pass through the
   * {@code Authorization} header from the incoming request.
   *
   * @param disableGzipCompression if gzip compression of requests should be disabled. This may be
   *     needed if the server can not communicate with gzip enabled
   * @return a builder to configure the client
   * @deprecated Use {@link #platformClient(HttpClientConfiguration)} instead.
   */
  @Deprecated
  public PlatformClientBuilder platformClient(boolean disableGzipCompression) {
    return platformClient(new HttpClientConfiguration().setGzipEnabled(!disableGzipCompression));
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
    return new ExternalClientBuilder(createClientBuilder(httpClientConfiguration), tracer);
  }

  /**
   * Starts creation of a client that calls APIs outside of the SDA SE Platform. This clients does
   * no header magic.
   *
   * @param disableGzipCompression if gzip compression of requests should be disabled. This may be
   *     needed if the server can not communicate with gzip enabled
   * @return a builder to configure the client
   * @deprecated Use {@link #externalClient(HttpClientConfiguration)} instead.
   */
  @Deprecated
  public ExternalClientBuilder externalClient(boolean disableGzipCompression) {
    return externalClient(new HttpClientConfiguration().setGzipEnabled(!disableGzipCompression));
  }

  private JerseyClientBuilder createClientBuilder(HttpClientConfiguration httpClientConfiguration) {
    JerseyClientConfiguration configuration = new JerseyClientConfiguration();
    configuration.setChunkedEncodingEnabled(httpClientConfiguration.isChunkedEncodingEnabled());
    configuration.setGzipEnabled(httpClientConfiguration.isGzipEnabled());
    configuration.setGzipEnabledForRequests(httpClientConfiguration.isGzipEnabledForRequests());
    return new JerseyClientBuilder(environment).using(configuration);
  }
}
