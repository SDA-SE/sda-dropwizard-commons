package org.sdase.commons.server.s3;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.s3.test.Config;
import org.sdase.commons.server.s3.test.TestApp;
import org.sdase.commons.server.s3.testing.S3ClassExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

class S3BundleSignerOverrideTest {

  @RegisterExtension
  @Order(0)
  static final S3ClassExtension S3 =
      S3ClassExtension.builder().putObject("bucket", "key", "data").build();

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<Config> DW =
      new DropwizardAppExtension<>(
          TestApp.class,
          null,
          randomPorts(),
          config("s3Config.endpoint", S3::getEndpoint),
          config("s3Config.accessKey", "access-key"),
          config("s3Config.secretKey", "secret-key"),
          config("s3Config.signerOverride", "S3SignerType"));

  @Test
  void shouldProvideClient() throws IOException {
    S3Client client = getClient();

    assertThat(
            client
                .getObject(GetObjectRequest.builder().bucket("bucket").key("key").build())
                .readAllBytes())
        .hasToString("data");
  }

  @Test
  void shouldGeneratePresignedUrl() {
    S3Client client = getClient();
    try (S3Presigner presigner = S3Presigner.builder().s3Client(client).build()) {

      PutObjectPresignRequest presignRequest =
          PutObjectPresignRequest.builder()
              .signatureDuration(Duration.ofMinutes(10)) // The URL will expire in 10 minutes.
              .build();

      PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
      URL presignedUrl = presignedRequest.url();
      assertThat(presignedUrl.toString()).contains("Signature=");
    }
  }

  private S3Client getClient() {
    TestApp app = DW.getApplication();
    return app.getS3Bundle().getClient();
  }
}
