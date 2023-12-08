package org.sdase.commons.server.s3.health;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;

import com.robothy.s3.jupiter.LocalS3;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.SortedSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.s3.S3Bundle;
import org.sdase.commons.server.s3.test.Config;
import org.sdase.commons.server.s3.test.S3WithHealthCheckTestApp;
import org.sdase.commons.server.s3.testing.S3ClassExtension;

@LocalS3
class S3WithHealthCheckIT {

  @RegisterExtension
  @Order(0)
  static final S3ClassExtension S3 = S3ClassExtension.builder().createBucket("testbucket").build();

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<Config> DW =
      new DropwizardAppExtension<>(
          S3WithHealthCheckTestApp.class,
          resourceFilePath("test-config.yml"),
          config("s3Config.endpoint", S3::getEndpoint),
          config("s3Bucket", "testbucket"),
          randomPorts());

  private S3WithHealthCheckTestApp app;
  private WebTarget adminTarget;

  @BeforeEach
  void init() {
    app = DW.getApplication();
    adminTarget = DW.client().target(String.format("http://localhost:%d/", DW.getAdminPort()));
  }

  @Test
  void healthCheckRegistryShouldContainHealthCheck() {
    SortedSet<String> checks = app.healthCheckRegistry().getNames();
    assertThat(checks)
        .isNotEmpty()
        .contains(S3Bundle.S3_HEALTH_CHECK_NAME)
        .doesNotContain(S3Bundle.S3_EXTERNAL_HEALTH_CHECK_NAME);
  }

  @Test
  void shouldReturnHealthCheckOk() {
    try (Response response =
        adminTarget.path("healthcheck").request(MediaType.APPLICATION_JSON_TYPE).get()) {
      String responseBody = response.readEntity(String.class);
      assertThat(response.getStatus()).withFailMessage(() -> responseBody).isEqualTo(HTTP_OK);
      assertThat(responseBody)
          .contains("\"" + S3Bundle.S3_HEALTH_CHECK_NAME + "\"")
          .doesNotContain("\"" + S3Bundle.S3_EXTERNAL_HEALTH_CHECK_NAME + "\"");
    }
  }
}

// {
// "OpenPolicyAgent":{"healthy":false,"message":"org.apache.http.conn.HttpHostConnectException:
// Connect to localhost:8181 [localhost/127.0.0.1] failed: Connection
// refused","duration":11,"timestamp":"2023-12-13T14:41:12.950Z"},
// "deadlocks":{"healthy":true,"duration":0,"timestamp":"2023-12-13T14:41:12.938Z"}
// }
