package org.sdase.commons.server.s3.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.robothy.s3.jupiter.LocalS3;
import java.util.Collections;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.s3.testing.S3ClassExtension;

@LocalS3
class S3HealthCheckUnhealthyIT {

  // @RegisterExtension // don't start!
  static final S3ClassExtension S3 = S3ClassExtension.builder().createBucket("testbucket").build();

  private final S3HealthCheck s3HealthCheck =
      new S3HealthCheck(S3.newClient(), new HashSet<>(Collections.singletonList("testbucket")));

  @Test
  void shouldBeUnhealthy() {
    assertThat(s3HealthCheck.execute().isHealthy()).isFalse();
  }
}
