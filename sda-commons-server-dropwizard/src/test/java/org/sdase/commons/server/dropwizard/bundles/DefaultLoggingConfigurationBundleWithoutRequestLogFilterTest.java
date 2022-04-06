package org.sdase.commons.server.dropwizard.bundles;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Configuration;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;
import org.sdase.commons.server.dropwizard.bundles.test.RequestLoggingTestApp;

class DefaultLoggingConfigurationBundleWithoutRequestLogFilterTest {

  @RegisterExtension
  static final DropwizardAppExtension<Configuration> DW =
      new DropwizardAppExtension<>(
          RequestLoggingTestApp.class, resourceFilePath("without-appenders-key-config.yaml"));

  @Test
  @StdIo
  void shouldHaveDefaultHealthcheckRequestLogs(StdOut out) {
    Response response =
        createAdminTarget().path("healthcheck/internal").request().buildGet().invoke();

    assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
    assertThat(out.capturedLines()).anyMatch(line -> line.contains("/healthcheck/internal"));
  }

  private WebTarget createAdminTarget() {
    return DW.client().target(String.format("http://localhost:%d/", DW.getAdminPort()));
  }
}
