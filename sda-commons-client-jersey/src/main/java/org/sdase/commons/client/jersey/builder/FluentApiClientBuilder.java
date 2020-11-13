package org.sdase.commons.client.jersey.builder;

import javax.ws.rs.client.ClientRequestFilter;
import org.glassfish.jersey.client.ClientProperties;
import org.sdase.commons.client.jersey.ApiHttpClientConfiguration;

/**
 * Entrypoint to the creation of API clients based on annotated interfaces.
 *
 * <p>This interfaces and it's inner interfaces describe the fluent API to create clients.
 *
 * <p>As default, the client will follow redirects so that redirect status codes are automatically
 * resolved by the client. Automatically following redirects only works well for 303 See Other
 * although the {@linkplain ClientProperties#FOLLOW_REDIRECTS documentation} describes 3xx
 * redirects. It is required to keep {@linkplain
 * io.dropwizard.client.JerseyClientConfiguration#setGzipEnabledForRequests(boolean) GZIP disabled
 * for requests} which is the default of the {@link ApiHttpClientConfiguration}.
 *
 * <p>Example usages:
 *
 * <pre>
 *   <code>
 *
 *     // backward compatible
 *     clientBuilder.platformClient(). // … as before
 *     clientBuilder.externalClient(). // … as before
 *
 *     // NEW direct access to api clients for platform use
 *     clientBuilder.apiClient(PlatformServiceApi.class)
 *         .withConfiguration(new ApiHttpClientConfiguration())
 *         .disableFollowRedirects() // optional
 *         .addFilter(myClientRequestFilter) // optional
 *         .withPlatformFeatures()
 *         .enableAuthenticationPassThrough() // optional
 *         .enableConsumerToken() // optional
 *         .build(); // or .build("my-custom-client-name")
 *
 *     // NEW direct access to api clients for external use
 *     clientBuilder.apiClient(PlatformServiceApi.class)
 *         .withConfiguration(new ApiHttpClientConfiguration())
 *         .disableFollowRedirects() // optional
 *         .addFilter(myClientRequestFilter) // optional
 *         .build(); // or .build("my-custom-client-name")
 *   </code>
 * </pre>
 */
public interface FluentApiClientBuilder {

  /**
   * Starts creation of a client based on an annotated interface.
   *
   * @param apiInterface the annotated interface that describes the API
   * @param <A> the type of the client interface
   * @return a builder to configure the API client
   */
  <A> ApiClientInstanceConfigurationBuilder<A> apiClient(Class<A> apiInterface);

  interface ApiClientInstanceConfigurationBuilder<A> {

    /**
     * Configures the client.
     *
     * @param apiHttpClientConfiguration the configuration of the client, usually referenced in the
     *     app configuration
     * @return a builder for implementation specific configuration
     */
    ApiClientCommonConfigurationBuilder<A> withConfiguration(
        ApiHttpClientConfiguration apiHttpClientConfiguration);
  }

  interface ApiClientCommonConfigurationBuilder<A> extends ApiClientFinalBuilder<A> {

    /**
     * Adds a request filter to the client.
     *
     * @param clientRequestFilter the filter to add
     * @return this builder instance
     */
    ApiClientCommonConfigurationBuilder<A> addFilter(ClientRequestFilter clientRequestFilter);

    /**
     * Set this client to not follow redirects and therewith automatically resolve 3xx status codes.
     *
     * @return this builder instance
     */
    ApiClientCommonConfigurationBuilder<A> disableFollowRedirects();

    /**
     * The created client is used to call APIs within the SDA SE Platform. This client automatically
     * send a {@code Trace-Token} from the incoming request or a new {@code Trace-Token} to the API
     * resources and can optionally send a {@code Consumer-Token} or pass through the {@code
     * Authorization} header from the incoming request.
     *
     * <p>These features shall never be enabled for clients that access APIs outside the same SDA
     * Platform instance where this service is deployed.
     *
     * @return a builder to configure the client
     */
    ApiPlatformClientConfigurationBuilder<A> enablePlatformFeatures();
  }

  interface ApiPlatformClientConfigurationBuilder<A> extends ApiClientFinalBuilder<A> {

    /**
     * If authentication pass through is enabled, the JWT in the {@value
     * javax.ws.rs.core.HttpHeaders#AUTHORIZATION} header of an incoming request will be added to
     * the outgoing request.
     *
     * @return this builder instance
     */
    ApiPlatformClientConfigurationBuilder<A> enableAuthenticationPassThrough();

    /**
     * If consumer token is enabled, the client will create a configured consumer token and add it
     * as header to the outgoing request.
     *
     * @return this builder instance
     */
    ApiPlatformClientConfigurationBuilder<A> enableConsumerToken();
  }

  interface ApiClientFinalBuilder<A> {

    /**
     * Builds the client with a default name for metrics and thread names derived from the API
     * interface. If multiple clients of the same API interface are used, a {@linkplain
     * #build(String) a unique custom name} must be used.
     *
     * @return the client
     */
    A build();

    /**
     * Builds the client with a custom name for metrics and thread names.
     *
     * @param customName the name used for metrics and thread names must be unique across all
     *     clients of the application
     * @return the client
     */
    A build(String customName);
  }
}
