package org.sdase.commons.client.jersey.builder;

import static org.sdase.commons.server.opentracing.client.ClientTracingUtil.registerTracing;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Environment;
import io.opentracing.Tracer;
import java.net.ProxySelector;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestFilter;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.glassfish.jersey.client.ClientProperties;
import org.sdase.commons.client.jersey.HttpClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builder that provides options that are common for all types of clients.
 *
 * @param <T> the type of the subclass
 */
abstract class AbstractBaseClientBuilder<T extends AbstractBaseClientBuilder<T>> {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractBaseClientBuilder.class);
  /**
   * As default, the client will follow redirects so that redirect status codes are automatically
   * resolved by the client.
   */
  private static final boolean DEFAULT_FOLLOW_REDIRECTS = true;

  private HttpClientConfiguration httpClientConfiguration;
  private JerseyClientBuilder jerseyClientBuilder;
  private final Tracer tracer;
  private final ObjectMapper objectMapper;

  private List<ClientRequestFilter> filters;

  private Integer connectionTimeoutMillis;

  private Integer readTimeoutMillis;

  private boolean followRedirects;

  AbstractBaseClientBuilder(
      Environment environment, HttpClientConfiguration httpClientConfiguration, Tracer tracer) {
    this.httpClientConfiguration = httpClientConfiguration;
    this.jerseyClientBuilder = new JerseyClientBuilder(environment);
    this.tracer = tracer;
    this.objectMapper = environment.getObjectMapper();
    this.filters = new ArrayList<>();
    this.followRedirects = DEFAULT_FOLLOW_REDIRECTS;

    // a specific proxy configuration always overrides the system proxy
    if (httpClientConfiguration.getProxyConfiguration() == null) {
      // register a route planner that uses the default proxy variables (e.g. http.proxyHost)
      this.jerseyClientBuilder.using(new SystemDefaultRoutePlanner(ProxySelector.getDefault()));
    }
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
    return (T) this;
  }

  /**
   * Builds a generic client that can be used for Http requests.
   *
   * @param name the name of the client is used for metrics and thread names
   * @return the client instance
   */
  public Client buildGenericClient(String name) {
    // Create a copy of the configuration
    final HttpClientConfiguration configuration =
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

    Client client = jerseyClientBuilder.using(configuration).build(name);
    filters.forEach(client::register);
    client.property(ClientProperties.FOLLOW_REDIRECTS, followRedirects);
    registerMultiPartIfAvailable(client);
    registerTracing(client, tracer);
    return client;
  }

  /**
   * Creates a client proxy implementation for accessing another service.
   *
   * @param apiInterface the interface that declares the API using JAX-RS annotations.
   * @param <A> the type of the api
   * @return a builder to define the root path of the API for the proxy that is build
   */
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
   */
  public <A> ApiClientBuilder<A> api(Class<A> apiInterface, String customName) {
    return new ApiClientBuilder<>(apiInterface, buildGenericClient(customName));
  }

  private void registerMultiPartIfAvailable(Client client) {
    try {
      ClassLoader classLoader = getClass().getClassLoader();
      Class<?> multiPartFeature =
          classLoader.loadClass("org.glassfish.jersey.media.multipart.MultiPartFeature");
      if (multiPartFeature != null) {
        LOG.info("Registering MultiPartFeature for client.");
        client.register(multiPartFeature);
      }
    } catch (ClassNotFoundException e) {
      LOG.info("Not registering MultiPartFeature for client: Class is not available.");
    } catch (Exception e) {
      LOG.warn("Failed to register MultiPartFeature for client.");
    }
  }
}
