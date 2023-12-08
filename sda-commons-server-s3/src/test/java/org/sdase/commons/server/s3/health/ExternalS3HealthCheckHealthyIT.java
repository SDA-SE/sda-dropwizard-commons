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
class ExternalS3HealthCheckHealthyIT {
  @RegisterExtension
  static final S3ClassExtension S3 = S3ClassExtension.builder().createBucket("testbucket").build();

  private ExternalS3HealthCheck externalS3HealthCheck;

  @BeforeEach
  void init() {
    externalS3HealthCheck =
        new ExternalS3HealthCheck(
            S3.newClient(), new HashSet<>(Collections.singletonList("testbucket")));
  }

  @Test
  void shouldBeHealthy() {
    assertThat(externalS3HealthCheck.execute().isHealthy()).isTrue();
  }
}
