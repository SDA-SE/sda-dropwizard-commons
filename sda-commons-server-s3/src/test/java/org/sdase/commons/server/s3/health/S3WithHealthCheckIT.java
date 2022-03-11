package org.sdase.commons.server.s3.health;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.SortedSet;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.sdase.commons.server.s3.S3Bundle;
import org.sdase.commons.server.s3.test.Config;
import org.sdase.commons.server.s3.test.S3WithHealthCheckTestApp;
import org.sdase.commons.server.s3.testing.S3MockRule;

public class S3WithHealthCheckIT {
  private static final S3MockRule S3_MOCK = S3MockRule.builder().createBucket("testbucket").build();

  private static final DropwizardAppRule<Config> DW =
      new DropwizardAppRule<>(
          S3WithHealthCheckTestApp.class,
          resourceFilePath("test-config.yml"),
          config("s3Config.endpoint", S3_MOCK::getEndpoint),
          config("s3Bucket", "testbucket"));

  @ClassRule public static final TestRule CHAIN = RuleChain.outerRule(S3_MOCK).around(DW);

  private S3WithHealthCheckTestApp app;
  private WebTarget adminTarget;

  @Before
  public void init() {
    app = DW.getApplication();
    adminTarget = DW.client().target(String.format("http://localhost:%d/", DW.getAdminPort()));
  }

  @Test
  public void healthCheckShouldNotContainS3() {
    SortedSet<String> checks = app.healthCheckRegistry().getNames();
    assertThat(checks).isNotEmpty().contains(S3Bundle.S3_HEALTH_CHECK_NAME);
  }

  @Test
  public void shouldReturnHealthCheckOk() {
    Response response = adminTarget.path("healthcheck").request().get();
    assertThat(response.getStatus()).isEqualTo(HTTP_OK);
    assertThat(response.readEntity(String.class))
        .contains("\"" + S3Bundle.S3_HEALTH_CHECK_NAME + "\"")
        .doesNotContain("\"" + S3Bundle.S3_EXTERNAL_HEALTH_CHECK_NAME + "\"");
  }
}
