package org.sdase.commons.client.jersey.builder;

import io.dropwizard.client.JerseyClientBuilder;
import io.opentracing.Tracer;

public class ExternalClientBuilder extends AbstractBaseClientBuilder<ExternalClientBuilder> {

  public ExternalClientBuilder(JerseyClientBuilder jerseyClientBuilder, Tracer tracer) {
    super(jerseyClientBuilder, tracer);
  }
}
