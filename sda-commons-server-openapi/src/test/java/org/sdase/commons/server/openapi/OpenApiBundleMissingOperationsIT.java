package org.sdase.commons.server.openapi;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

import io.dropwizard.Configuration;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;
import org.sdase.commons.server.openapi.apps.missingoperation.MissingOperationTestApp;

class OpenApiBundleMissingOperationsIT {

  @RegisterExtension
  public static final DropwizardAppExtension<Configuration> DW =
      new DropwizardAppExtension<>(
          MissingOperationTestApp.class, resourceFilePath("test-config.yaml"));

  private static Builder getYamlRequest() {
    return DW.client()
        .target(getTarget())
        .path("/api")
        .path("/openapi.yaml")
        .request("application/yaml");
  }

  private static String getTarget() {
    return "http://localhost:" + DW.getLocalPort();
  }

  @Test
  @StdIo
  void shouldProvideValidYaml(StdOut out) {
    try (Response response = getYamlRequest().get()) {
      assertThat(response.getStatus()).isEqualTo(OK_200);
      assertThat(response.getMediaType()).isEqualTo(MediaType.valueOf("application/yaml"));

      String output = String.join("\n", out.capturedLines());
      assertThat(output).doesNotContain("NullPointerException");
    }
  }
}
