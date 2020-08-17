package org.sdase.commons.client.jersey.builder;

import io.dropwizard.setup.Environment;
import io.opentracing.Tracer;
import org.sdase.commons.client.jersey.HttpClientConfiguration;

public class ExternalClientBuilder extends AbstractBaseClientBuilder<ExternalClientBuilder> {

  public ExternalClientBuilder(
      Environment environment, HttpClientConfiguration httpClientConfiguration, Tracer tracer) {
    super(environment, httpClientConfiguration, tracer);
  }
}
