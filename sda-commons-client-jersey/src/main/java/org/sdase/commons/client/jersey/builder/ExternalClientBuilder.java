package org.sdase.commons.client.jersey.builder;

import io.dropwizard.setup.Environment;
import io.opentelemetry.api.OpenTelemetry;
import org.sdase.commons.client.jersey.HttpClientConfiguration;

public class ExternalClientBuilder extends AbstractBaseClientBuilder<ExternalClientBuilder> {

  public ExternalClientBuilder(
      Environment environment,
      HttpClientConfiguration httpClientConfiguration,
      OpenTelemetry openTelemetry) {
    super(environment, httpClientConfiguration, openTelemetry);
  }
}
