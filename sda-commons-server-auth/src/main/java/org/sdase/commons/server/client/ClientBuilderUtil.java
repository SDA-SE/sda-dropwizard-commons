package org.sdase.commons.server.client;

import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.core.setup.Environment;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.apachehttpclient.v5_2.ApacheHttpClientTelemetry;

public class ClientBuilderUtil {

  private ClientBuilderUtil() {
    // this is a utility
  }

  public static HttpClientBuilder createJerseyClientBuilder(
      Environment environment, OpenTelemetry openTelemetry) {
    return new HttpClientBuilder(environment) {
      @Override
      protected org.apache.hc.client5.http.impl.classic.HttpClientBuilder createBuilder() {
        return ApacheHttpClientTelemetry.builder(openTelemetry).build().createHttpClientBuilder();
      }
    };
  }
}
