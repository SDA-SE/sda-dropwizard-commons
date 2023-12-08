package org.sdase.commons.server.s3.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.robothy.s3.jupiter.LocalS3;
import java.util.Collections;
import java.util.HashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.s3.testing.S3ClassExtension;

@LocalS3
class S3HealthCheckHealthyIT {

  @RegisterExtension
  static final S3ClassExtension S3 = S3ClassExtension.builder().createBucket("testbucket").build();

  private S3HealthCheck s3HealthCheck;

  @BeforeEach
  void init() {
    s3HealthCheck =
        new S3HealthCheck(S3.newClient(), new HashSet<>(Collections.singletonList("testbucket")));
  }

  @Test
  void shouldBeHealthy() {
    assertThat(s3HealthCheck.execute().isHealthy()).isTrue();
  }
}
