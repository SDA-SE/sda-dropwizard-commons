package org.sdase.commons.starter.example;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sdase.commons.server.opa.testing.OpaRule.onAnyRequest;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sdase.commons.server.auth.testing.AuthClassExtension;
import org.sdase.commons.server.opa.testing.OpaClassExtension;
import org.sdase.commons.starter.SdaPlatformConfiguration;

class PlatformRequirementsIT {

  @Order(0)
  @RegisterExtension
  static final OpaClassExtension OPA = new OpaClassExtension();

  @Order(1)
  @RegisterExtension
  @SuppressWarnings("unused")
  static final AuthClassExtension AUTH = AuthClassExtension.builder().withDisabledAuth().build();

  @Order(2)
  @RegisterExtension
  static final DropwizardAppExtension<SdaPlatformConfiguration> DW =
      new DropwizardAppExtension<>(
          SdaPlatformExampleApplication.class,
          resourceFilePath("test-config.yaml"),
          config("opa.baseUrl", OPA::getUrl));

  @BeforeAll
  static void beforeAll() {
    OPA.mock(onAnyRequest().deny());
  }

  @ParameterizedTest
  @ValueSource(strings = {"openapi.json", "openapi.yaml"})
  void shouldHavePublicAppEndpoint(String path) {
    Response r = createTarget().path(path).request().get();
    assertThat(r.getStatus()).isEqualTo(OK.getStatusCode());
  }

  @ParameterizedTest
  @ValueSource(strings = {"ping", "metrics/prometheus", "healthcheck/internal"})
  void shouldHaveAdminEndpoints(String path) {
    Response response = createAdminTarget().path(path).request().buildGet().invoke();
    assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
  }

  private WebTarget createTarget() {
    return DW.client().target(String.format("http://localhost:%d/", DW.getLocalPort()));
  }

  private WebTarget createAdminTarget() {
    return DW.client().target(String.format("http://localhost:%d/", DW.getAdminPort()));
  }
}
