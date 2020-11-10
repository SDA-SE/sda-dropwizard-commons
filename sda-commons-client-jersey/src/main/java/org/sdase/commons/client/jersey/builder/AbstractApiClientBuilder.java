package org.sdase.commons.client.jersey.builder;

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Environment;
import io.opentracing.Tracer;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.ClientRequestFilter;
import org.sdase.commons.client.jersey.ApiHttpClientConfiguration;

public class AbstractApiClientBuilder<T extends AbstractApiClientBuilder<T>> {

  /**
   * As default, the client will follow redirects so that redirect status codes are automatically
   * resolved by the client.
   */
  private static final boolean DEFAULT_FOLLOW_REDIRECTS = true;

  private ApiHttpClientConfiguration apiHttpClientConfiguration;
  private InternalJerseyClientFactory internalJerseyClientFactory;
  private List<ClientRequestFilter> filters;
  private boolean followRedirects;

  AbstractApiClientBuilder(
      Environment environment,
      ApiHttpClientConfiguration apiHttpClientConfiguration,
      Tracer tracer) {
    this.apiHttpClientConfiguration = apiHttpClientConfiguration;
    this.internalJerseyClientFactory =
        new InternalJerseyClientFactory(new JerseyClientBuilder(environment), tracer);
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
   * Creates a client proxy implementation for accessing another service.
   *
   * @param apiInterface the interface that declares the API using JAX-RS annotations.
   * @param <A> the type of the api
   * @return the client proxy implementing the client interface
   */
  public <A> A api(Class<A> apiInterface) {
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
   * @return the client proxy implementing the client interface
   */
  public <A> A api(Class<A> apiInterface, String customName) {
    return new ApiClientBuilder<>(
            apiInterface,
            internalJerseyClientFactory.createClient(
                customName, apiHttpClientConfiguration, filters, followRedirects))
        .atTarget(apiHttpClientConfiguration.getApiBaseUrl());
  }
}
