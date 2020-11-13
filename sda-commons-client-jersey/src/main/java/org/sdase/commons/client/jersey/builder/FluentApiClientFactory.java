package org.sdase.commons.client.jersey.builder;

import static org.sdase.commons.client.jersey.proxy.ApiClientInvocationHandler.createProxy;

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Environment;
import io.opentracing.Tracer;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.sdase.commons.client.jersey.ApiHttpClientConfiguration;
import org.sdase.commons.client.jersey.filter.AuthHeaderClientFilter;
import org.sdase.commons.client.jersey.filter.ConsumerTokenHeaderFilter;
import org.sdase.commons.client.jersey.filter.TraceTokenClientFilter;

public class FluentApiClientFactory<A>
    implements FluentApiClientBuilder.ApiClientInstanceConfigurationBuilder<A>,
        FluentApiClientBuilder.ApiPlatformClientConfigurationBuilder<A>,
        FluentApiClientBuilder.ApiClientCommonConfigurationBuilder<A>,
        FluentApiClientBuilder.ApiClientFinalBuilder<A> {

  private final Class<A> api;
  private final InternalJerseyClientFactory internalJerseyClientFactory;
  private final String consumerToken;
  private final List<ClientRequestFilter> filters = new ArrayList<>();
  private ApiHttpClientConfiguration apiHttpClientConfiguration;
  private boolean followRedirects = true;

  public FluentApiClientFactory(
      Class<A> api, Environment environment, String consumerToken, Tracer tracer) {
    this.api = api;
    this.consumerToken = consumerToken;
    this.internalJerseyClientFactory =
        new InternalJerseyClientFactory(new JerseyClientBuilder(environment), tracer);
  }

  @Override
  public FluentApiClientBuilder.ApiClientCommonConfigurationBuilder<A> withConfiguration(
      ApiHttpClientConfiguration apiHttpClientConfiguration) {
    this.apiHttpClientConfiguration = apiHttpClientConfiguration;
    return this;
  }

  @Override
  public FluentApiClientBuilder.ApiPlatformClientConfigurationBuilder<A> enablePlatformFeatures() {
    this.filters.add(0, new TraceTokenClientFilter());
    return this;
  }

  @Override
  public FluentApiClientBuilder.ApiClientCommonConfigurationBuilder<A> addFilter(
      ClientRequestFilter clientRequestFilter) {
    this.filters.add(clientRequestFilter);
    return this;
  }

  @Override
  public FluentApiClientBuilder.ApiClientCommonConfigurationBuilder<A> disableFollowRedirects() {
    this.followRedirects = false;
    return this;
  }

  @Override
  public FluentApiClientBuilder.ApiPlatformClientConfigurationBuilder<A>
      enableAuthenticationPassThrough() {
    if (isFilterMissing(AuthHeaderClientFilter.class)) {
      this.filters.add(new AuthHeaderClientFilter());
    }
    return this;
  }

  @Override
  public FluentApiClientBuilder.ApiPlatformClientConfigurationBuilder<A> enableConsumerToken() {
    if (isFilterMissing(ConsumerTokenHeaderFilter.class)) {
      this.filters.add(new ConsumerTokenHeaderFilter(consumerToken));
    }
    return this;
  }

  @Override
  public A build() {
    return build(api.getSimpleName());
  }

  @Override
  public A build(String customName) {
    Client client =
        internalJerseyClientFactory.createClient(
            customName, this.apiHttpClientConfiguration, this.filters, this.followRedirects);
    WebTarget webTarget = client.target(apiHttpClientConfiguration.getApiBaseUrl());
    A clientProxy = WebResourceFactory.newResource(this.api, webTarget);
    return createProxy(this.api, clientProxy);
  }

  private boolean isFilterMissing(Class<? extends ClientRequestFilter> filterType) {
    return this.filters.stream().map(Object::getClass).noneMatch(filterType::equals);
  }
}
