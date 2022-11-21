package org.sdase.commons.server.openapi.example.people.rest;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.RetryingTest;
import org.sdase.commons.server.auth.testing.AuthClassExtension;
import org.sdase.commons.server.openapi.example.OpenApiExampleApplication;
import org.sdase.commons.starter.SdaPlatformConfiguration;

// This is a simple integration test that checks whether the swagger documentation is produced at
// the right path, however doesn't test the contents of the documentation.
class OpenApiIT {

  // create a dummy authentication provider that works as a local OpenId
  // Connect provider for the tests
  @RegisterExtension
  @Order(0)
  private static final AuthClassExtension AUTH = AuthClassExtension.builder().build();

  @RegisterExtension
  @Order(1)
  public static final DropwizardAppExtension<SdaPlatformConfiguration> DW =
      // Setup a test instance of the application
      new DropwizardAppExtension<>(
          OpenApiExampleApplication.class,
          // use the config file 'test-config.yaml' from the test resources folder
          resourceFilePath("test-config.yaml"));

  @Test
  @RetryingTest(5)
  void testAnswerOpenApiJson() {
    // given

    // when
    try (final Response r = baseUrlWebTarget().path("openapi.json").request().get()) {
      // then
      assertThat(r.getStatus()).isEqualTo(OK.getStatusCode());
    }
  }

  @Test
  @RetryingTest(5)
  void testAnswerOpenApiYaml() {
    // given

    // when
    try (final Response r = baseUrlWebTarget().path("openapi.yaml").request().get()) {
      // then
      assertThat(r.getStatus()).isEqualTo(OK.getStatusCode());
    }
  }

  private WebTarget baseUrlWebTarget() {
    return DW.client().target(String.format("http://localhost:%d", DW.getLocalPort()));
  }
}
