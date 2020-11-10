package org.sdase.commons.client.jersey.builder;

import io.dropwizard.setup.Environment;
import io.opentracing.Tracer;
import org.sdase.commons.client.jersey.ApiHttpClientConfiguration;

public class ExternalApiClientBuilder extends AbstractApiClientBuilder<ExternalApiClientBuilder> {

  public ExternalApiClientBuilder(
      Environment environment,
      ApiHttpClientConfiguration apiHttpClientConfiguration,
      Tracer tracer) {
    super(environment, apiHttpClientConfiguration, tracer);
  }
}
