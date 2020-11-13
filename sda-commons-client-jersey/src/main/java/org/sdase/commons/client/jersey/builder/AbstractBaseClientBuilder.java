package org.sdase.commons.client.jersey.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Environment;
import io.opentracing.Tracer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestFilter;
import org.glassfish.jersey.client.ClientProperties;
import org.sdase.commons.client.jersey.HttpClientConfiguration;

/**
 * Builder that provides options that are common for all types of clients.
 *
 * @param <T> the type of the subclass
 */
abstract class AbstractBaseClientBuilder<T extends AbstractBaseClientBuilder<T>> {

  /**
   * As default, the client will follow redirects so that redirect status codes are automatically
   * resolved by the client. Automatically following redirects only works well for 303 See Other
   * although the {@linkplain ClientProperties#FOLLOW_REDIRECTS documentation} describes 3xx
   * redirects. It is required to keep {@linkplain
   * HttpClientConfiguration#setGzipEnabledForRequests(boolean) GZIP disabled for requests} which is
   * the default of the {@link HttpClientConfiguration}.
   */
  private static final boolean DEFAULT_FOLLOW_REDIRECTS = true;

  private HttpClientConfiguration httpClientConfiguration;
  private InternalJerseyClientFactory internalJerseyClientFactory;
  private final ObjectMapper objectMapper;

  private List<ClientRequestFilter> filters;

  private Integer connectionTimeoutMillis;

  private Integer readTimeoutMillis;

  private boolean followRedirects;

  AbstractBaseClientBuilder(
      Environment environment, HttpClientConfiguration httpClientConfiguration, Tracer tracer) {
    this.httpClientConfiguration = httpClientConfiguration;
    this.internalJerseyClientFactory =
        new InternalJerseyClientFactory(new JerseyClientBuilder(environment), tracer);
    this.objectMapper = environment.getObjectMapper();
    this.filters = new ArrayList<>();
    this.followRedirects = DEFAULT_FOLLOW_REDIRECTS;
  }

  /**
   * Adds a request filter to the client.
   *
   * @param clientRequestFilter the filter to add
   * @return this builder instance
   */
  public T addFilter(ClientRequestFilter clientRequestFilter) {
    this.filters.add(clientRequestFilter);
    //noinspection unchecked
    return (T) this;
  }

  /**
   * Sets the connection timeout for the clients that are built with this instance. The connection
   * timeout is the amount of time to wait until the connection to the server is established. The
   * default is {@value
   * org.sdase.commons.client.jersey.HttpClientConfiguration#DEFAULT_CONNECTION_TIMEOUT_MS}ms.
   *
   * <p>If the connection timeout is overdue a {@link javax.ws.rs.ProcessingException} wrapping a
   * {@link org.apache.http.conn.ConnectTimeoutException} is thrown by the client.
   *
   * @param connectionTimeout the time to wait until a connection to the remote service is
   *     established
   * @return this builder instance
   * @deprecated The client is now configurable with the Dropwizard configuration. Please use {@link
   *     HttpClientConfiguration#setConnectionTimeout} instead.
   */
  @Deprecated
  public T withConnectionTimeout(Duration connectionTimeout) {
    this.connectionTimeoutMillis = (int) connectionTimeout.toMillis();
    //noinspection unchecked
    return (T) this;
  }

  /**
   * Sets the read timeout for the clients that are built with this instance. The read timeout is
   * the timeout to wait for data in an established connection. Usually this timeout is violated
   * when the client has sent the request and is waiting for the first byte of the response while
   * the server is doing calculations, accessing a database or delegating to other services. The
   * default is {@value
   * org.sdase.commons.client.jersey.HttpClientConfiguration#DEFAULT_TIMEOUT_MS}ms. The read timeout
   * should be set wisely according to the use case considering how long a user is willing to wait
   * and how long backend operations need.
   *
   * <p>If the connection timeout is overdue a {@link javax.ws.rs.ProcessingException} wrapping a
   * {@link java.net.SocketTimeoutException} is thrown by the client.
   *
   * @param readTimeout the time to wait for content in an established connection
   * @return this builder instance
   * @deprecated The client is now configurable with the Dropwizard configuration. Please use {@link
   *     HttpClientConfiguration#setTimeout} instead.
   */
  @Deprecated
  public T withReadTimeout(Duration readTimeout) {
    this.readTimeoutMillis = (int) readTimeout.toMillis();
    //noinspection unchecked
    return (T) this;
  }

  /**
   * Set this client to not follow redirects and therewith automatically resolve 3xx status codes
   *
   * @return this builder instance
   */
  public T disableFollowRedirects() {
    this.followRedirects = false;
    //noinspection unchecked
    return (T) this;
  }

  /**
   * Builds a generic client that can be used for Http requests.
   *
   * @param name the name of the client is used for metrics and thread names
   * @return the client instance
   */
  public Client buildGenericClient(String name) {
    HttpClientConfiguration configuration = createBackwardCompatibleHttpClientConfiguration();
    return internalJerseyClientFactory.createClient(
        name, configuration, this.filters, this.followRedirects);
  }

  /**
   * Creates a client proxy implementation for accessing another service.
   *
   * @param apiInterface the interface that declares the API using JAX-RS annotations.
   * @param <A> the type of the api
   * @return a builder to define the root path of the API for the proxy that is build
   * @deprecated a {@linkplain org.sdase.commons.client.jersey.ClientFactory#apiClient(Class) new
   *     API} with a dedicated {@linkplain
   *     org.sdase.commons.client.jersey.ApiHttpClientConfiguration configuration class} is
   *     available to configure API clients based on interfaces. The new {@link
   *     FluentApiClientBuilder} API provides the same features as this builder but uses all
   *     configurable options in a the dedicated {@link
   *     org.sdase.commons.client.jersey.ApiHttpClientConfiguration} and therefore enforces to have
   *     all configuration options (like proxy settings) available for operators who need them.
   */
  @Deprecated
  public <A> ApiClientBuilder<A> api(Class<A> apiInterface) {
    return api(apiInterface, apiInterface.getSimpleName());
  }

  /**
   * Creates a client proxy implementation for accessing another service. Allows to set a custom
   * name if required, e.g. if you have multiple clients generated from the same interface.
   *
   * @param apiInterface the interface that declares the API using JAX-RS annotations.
   * @param customName the custom name to use for the client. The name is used for the executor
   *     service and metrics. Names have to be unique.
   * @param <A> the type of the api
   * @return a builder to define the root path of the API for the proxy that is build
   * @deprecated a {@linkplain org.sdase.commons.client.jersey.ClientFactory#apiClient(Class) new
   *     API} with a dedicated {@linkplain
   *     org.sdase.commons.client.jersey.ApiHttpClientConfiguration configuration class} is
   *     available to configure API clients based on interfaces. The new {@link
   *     FluentApiClientBuilder} API provides the same features as this builder but uses all
   *     configurable options in a the dedicated {@link
   *     org.sdase.commons.client.jersey.ApiHttpClientConfiguration} and therefore enforces to have
   *     all configuration options (like proxy settings) available for operators who need them.
   */
  @Deprecated
  public <A> ApiClientBuilder<A> api(Class<A> apiInterface, String customName) {
    return new ApiClientBuilder<>(apiInterface, buildGenericClient(customName));
  }

  /**
   * @return a copy of the {@link #httpClientConfiguration} using timeouts from deprecated
   *     configuration if needed
   */
  private HttpClientConfiguration createBackwardCompatibleHttpClientConfiguration() {
    // Create a copy of the configuration
    HttpClientConfiguration configuration =
        objectMapper.convertValue(httpClientConfiguration, httpClientConfiguration.getClass());

    // This is a legacy option that should be removed if the connectionTimeout is no longer
    // configurable directly.
    if (connectionTimeoutMillis != null) {
      configuration.setConnectionTimeout(
          io.dropwizard.util.Duration.milliseconds(connectionTimeoutMillis));
    }
    // This is a legacy option that should be removed if the readTimeout is no longer configurable
    // directly.
    if (readTimeoutMillis != null) {
      configuration.setTimeout(io.dropwizard.util.Duration.milliseconds(readTimeoutMillis));
    }
    return configuration;
  }
}
