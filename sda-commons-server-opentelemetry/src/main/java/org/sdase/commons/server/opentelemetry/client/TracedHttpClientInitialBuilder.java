package org.sdase.commons.server.opentelemetry.client;

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Environment;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.apachehttpclient.v4_3.ApacheHttpClientTelemetry;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * A {@link HttpClientBuilder} that is enhanced with tracing capabilities. This can be used with
 * {@link JerseyClientBuilder#setApacheHttpClientBuilder(io.dropwizard.client.HttpClientBuilder)}
 * and preferably be set as soon as creating a new {@link JerseyClientBuilder} instance in order to
 * avoid rewriting custom configuration.
 */
public class TracedHttpClientInitialBuilder extends io.dropwizard.client.HttpClientBuilder {
  private OpenTelemetry openTelemetry;

  public TracedHttpClientInitialBuilder(Environment environment) {
    super(environment);
  }

  public TracedHttpClientInitialBuilder usingTelemetryInstance(OpenTelemetry openTelemetry) {
    this.openTelemetry = openTelemetry;
    return this;
  }

  @Override
  protected HttpClientBuilder createBuilder() {
    return ApacheHttpClientTelemetry.builder(openTelemetry).build().newHttpClientBuilder();
  }
}
