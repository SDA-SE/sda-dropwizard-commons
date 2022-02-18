package org.sdase.commons.server.s3.health;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.SortedSet;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.sdase.commons.server.s3.S3Bundle;
import org.sdase.commons.server.s3.test.Config;
import org.sdase.commons.server.s3.test.S3WithExternalHealthCheckTestApp;
import org.sdase.commons.server.s3.testing.S3MockRule;

public class S3WithoutHealthCheckIT {

  private static final S3MockRule S3_MOCK = S3MockRule.builder().build();

  private static final DropwizardAppRule<Config> DW =
      new DropwizardAppRule<>(
          S3WithExternalHealthCheckTestApp.class,
          resourceFilePath("test-config.yml"),
          config("s3Config.endpoint", S3_MOCK::getEndpoint));

  @ClassRule public static final TestRule CHAIN = RuleChain.outerRule(S3_MOCK).around(DW);

  private S3WithExternalHealthCheckTestApp app;

  @Before
  public void init() {
    app = DW.getApplication();
  }

  @Test
  public void healthCheckShouldNotContainS3() {
    SortedSet<String> checks = app.healthCheckRegistry().getNames();
    assertThat(checks).isNotEmpty().doesNotContain(S3Bundle.S3_HEALTH_CHECK_NAME);
  }
}
