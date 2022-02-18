package org.sdase.commons.server.s3.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.s3.testing.S3MockRule;

public class S3HealthCheckIT {

  @ClassRule
  public static final S3MockRule S3_MOCK = S3MockRule.builder().createBucket("testbucket").build();

  private S3HealthCheck s3HealthCheck;

  @Before
  public void init() {
    s3HealthCheck = new S3HealthCheck(S3_MOCK.getClient(), new String[] {"testbucket"});
  }

  @Test
  public void shouldBeHealthy() {
    assertThat(s3HealthCheck.execute().isHealthy()).isTrue();
  }

  @Test
  public void shouldBeUnhealthy() {
    S3_MOCK.stop();
    assertThat(s3HealthCheck.execute().isHealthy()).isFalse();
    S3_MOCK.start();
  }
}
