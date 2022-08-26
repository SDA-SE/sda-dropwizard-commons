package org.sdase.commons.client.jersey.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Environment;
import io.opentelemetry.api.OpenTelemetry;
import java.net.ProxySelector;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Feature;
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
  private final ObjectMapper objectMapper;
  private OpenTelemetry openTelemetry;

  private final List<ClientRequestFilter> filters;
  private final List<Feature> features;

  private boolean followRedirects;

  AbstractBaseClientBuilder(
      Environment environment,
      HttpClientConfiguration httpClientConfiguration,
      OpenTelemetry openTelemetry) {
    this.httpClientConfiguration = httpClientConfiguration;
    this.jerseyClientBuilder = new JerseyClientBuilder(environment);
    this.openTelemetry = openTelemetry;
    this.jerseyClientBuilder.setApacheHttpClientBuilder(
        new OtelHttpClientBuilder(environment).usingTelemetryInstance(this.openTelemetry));
    this.objectMapper = environment.getObjectMapper();
    this.filters = new ArrayList<>();
    this.features = new ArrayList<>();
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
   * Adds a feature used by the client.
   *
   * @param feature to be added
   * @return this builder instance
   */
  public T addFeature(Feature feature) {
    this.features.add(feature);
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
    // Create a copy of the configuration
    final HttpClientConfiguration configuration =
        objectMapper.convertValue(httpClientConfiguration, httpClientConfiguration.getClass());

    Client client = jerseyClientBuilder.using(configuration).build(name);
    filters.forEach(client::register);
    features.forEach(client::register);
    client.property(ClientProperties.FOLLOW_REDIRECTS, followRedirects);
    registerMultiPartIfAvailable(client);
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
