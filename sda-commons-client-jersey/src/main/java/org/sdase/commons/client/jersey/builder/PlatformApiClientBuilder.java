package org.sdase.commons.client.jersey.builder;

import io.dropwizard.setup.Environment;
import io.opentracing.Tracer;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.sdase.commons.client.jersey.ApiHttpClientConfiguration;
import org.sdase.commons.client.jersey.filter.AddRequestHeaderFilter;
import org.sdase.commons.client.jersey.filter.AuthHeaderClientFilter;
import org.sdase.commons.client.jersey.filter.TraceTokenClientFilter;
import org.sdase.commons.shared.tracing.ConsumerTracing;

public class PlatformApiClientBuilder extends AbstractApiClientBuilder<PlatformApiClientBuilder> {

  private Supplier<Optional<String>> consumerTokenSupplier;

  public PlatformApiClientBuilder(
      Environment environment,
      ApiHttpClientConfiguration apiHttpClientConfiguration,
      Tracer tracer,
      String consumerToken) {
    super(environment, apiHttpClientConfiguration, tracer);
    this.consumerTokenSupplier = () -> Optional.ofNullable(StringUtils.trimToNull(consumerToken));
    addFilter(new TraceTokenClientFilter());
  }

  /**
   * If authentication pass through is enabled, the JWT in the {@value
   * javax.ws.rs.core.HttpHeaders#AUTHORIZATION} header of an incoming request will be added to the
   * outgoing request.
   *
   * @return this builder instance
   */
  public PlatformApiClientBuilder enableAuthenticationPassThrough() {
    return addFilter(new AuthHeaderClientFilter());
  }

  /**
   * If consumer token is enabled, the client will create a configured consumer token and add it as
   * header to the outgoing request.
   *
   * @return this builder instance
   */
  public PlatformApiClientBuilder enableConsumerToken() {
    return addFilter(
        new AddRequestHeaderFilter() {
          @Override
          public String getHeaderName() {
            return ConsumerTracing.TOKEN_HEADER;
          }

          @Override
          public Optional<String> getHeaderValue() {
            return consumerTokenSupplier.get();
          }
        });
  }
}
