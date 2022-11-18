package org.sdase.commons.server.s3.health;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.SortedSet;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.s3.S3Bundle;
import org.sdase.commons.server.s3.test.Config;
import org.sdase.commons.server.s3.test.S3WithExternalHealthCheckTestApp;
import org.sdase.commons.server.s3.testing.S3ClassExtension;

class S3WithExternalHealthCheckIT {

  @RegisterExtension
  @Order(0)
  private static final S3ClassExtension S3 =
      S3ClassExtension.builder().createBucket("testbucket").build();

  @RegisterExtension
  @Order(1)
  private static final DropwizardAppExtension<Config> DW =
      new DropwizardAppExtension<>(
          S3WithExternalHealthCheckTestApp.class,
          resourceFilePath("test-config.yml"),
          config("s3Config.endpoint", S3::getEndpoint),
          config("s3Bucket", "testbucket"));

  private S3WithExternalHealthCheckTestApp app;
  private WebTarget adminTarget;

  @BeforeEach
  void init() {
    app = DW.getApplication();
    adminTarget = DW.client().target(String.format("http://localhost:%d/", DW.getAdminPort()));
  }

  @Test
  void healthCheckShouldNotContainS3() {
    SortedSet<String> checks = app.healthCheckRegistry().getNames();
    assertThat(checks).isNotEmpty().doesNotContain(S3Bundle.S3_HEALTH_CHECK_NAME);
  }

  @Test
  void shouldReturnHealthCheckOk() {
    Response response = adminTarget.path("healthcheck").request().get();
    assertThat(response.getStatus()).isEqualTo(HTTP_OK);
    assertThat(response.readEntity(String.class))
        .contains("\"" + S3Bundle.S3_EXTERNAL_HEALTH_CHECK_NAME + "\"")
        .doesNotContain("\"" + S3Bundle.S3_HEALTH_CHECK_NAME + "\"");
  }
}
