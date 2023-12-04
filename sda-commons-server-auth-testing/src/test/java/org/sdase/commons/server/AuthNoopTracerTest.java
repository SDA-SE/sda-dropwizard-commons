package org.sdase.commons.server;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sdase.commons.server.opa.testing.AbstractOpa.onAnyRequest;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.auth.testing.AuthClassExtension;
import org.sdase.commons.server.opa.testing.OpaClassExtension;
import org.sdase.commons.server.opa.testing.test.AuthAndOpaBundeTestAppConfiguration;
import org.sdase.commons.server.opa.testing.test.AuthAndOpaBundleTestApp;

class AuthNoopTracerTest {

  @Order(0)
  @RegisterExtension
  static final AuthClassExtension AUTH = AuthClassExtension.builder().build();

  @RegisterExtension
  @Order(1)
  static final OpaClassExtension OPA = new OpaClassExtension();

  @RegisterExtension
  @Order(2)
  static final DropwizardAppExtension<AuthAndOpaBundeTestAppConfiguration> DW =
      new DropwizardAppExtension<>(
          AuthAndOpaBundleTestApp.class,
          resourceFilePath("test-config.yaml"),
          config("opa.baseUrl", OPA::getUrl));

  @BeforeEach
  void makeRequest() {
    OPA.mock(onAnyRequest().allow());
    try (var response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort()) // NOSONAR
            .path("resources")
            .request()
            .headers(AUTH.auth().buildAuthHeader())
            .get()) {
      assertThat(response.getStatus()).isEqualTo(200);
    }
    OPA.verify(1, "GET", "/resources");
  }

  @Test
  void tracerShouldBeNoop() throws ClassNotFoundException {
    OpenTelemetry actual = GlobalOpenTelemetry.get();
    assertThat(actual)
        .extracting("delegate")
        .extracting("propagators")
        .extracting("textMapPropagator")
        .isInstanceOf(Class.forName("io.opentelemetry.context.propagation.NoopTextMapPropagator"));
  }
}
