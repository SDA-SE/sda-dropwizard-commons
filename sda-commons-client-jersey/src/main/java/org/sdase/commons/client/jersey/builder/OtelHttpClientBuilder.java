package org.sdase.commons.client.jersey.builder;

import io.dropwizard.setup.Environment;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.apachehttpclient.v4_3.ApacheHttpClientTelemetry;
import org.apache.http.impl.client.HttpClientBuilder;

public class OtelHttpClientBuilder extends io.dropwizard.client.HttpClientBuilder {
  private OpenTelemetry openTelemetry;

  public OtelHttpClientBuilder(Environment environment) {
    super(environment);
  }

  public OtelHttpClientBuilder usingTelemetryInstance(OpenTelemetry openTelemetry) {
    this.openTelemetry = openTelemetry;
    return this;
  }

  @Override
  protected HttpClientBuilder createBuilder() {
    return ApacheHttpClientTelemetry.builder(openTelemetry).build().newHttpClientBuilder();
  }
}
