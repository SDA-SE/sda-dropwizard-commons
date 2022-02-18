package org.sdase.commons.server.s3.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.s3.testing.S3MockRule;

public class ExternalS3HealthCheckIT {
  @ClassRule
  public static final S3MockRule S3_MOCK = S3MockRule.builder().createBucket("testbucket").build();

  private ExternalS3HealthCheck externalS3HealthCheck;

  @Before
  public void init() {
    externalS3HealthCheck =
        new ExternalS3HealthCheck(S3_MOCK.getClient(), new String[] {"testbucket"});
  }

  @Test
  public void shouldBeHealthy() {
    assertThat(externalS3HealthCheck.execute().isHealthy()).isTrue();
  }

  @Test
  public void shouldBeUnhealthy() {
    S3_MOCK.stop();
    assertThat(externalS3HealthCheck.execute().isHealthy()).isFalse();
    S3_MOCK.start();
  }
}
