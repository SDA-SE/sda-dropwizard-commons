package org.sdase.commons.server.s3;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.services.s3.AmazonS3;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.Date;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.s3.test.Config;
import org.sdase.commons.server.s3.test.TestApp;
import org.sdase.commons.server.s3.testing.S3ClassExtension;

class S3BundleSignerOverrideTest {

  @RegisterExtension
  @Order(0)
  private static final S3ClassExtension S3 =
      S3ClassExtension.builder().putObject("bucket", "key", "data").build();

  @RegisterExtension
  @Order(1)
  private static final DropwizardAppExtension<Config> DW =
      new DropwizardAppExtension<>(
          TestApp.class,
          null,
          randomPorts(),
          config("s3Config.endpoint", S3::getEndpoint),
          config("s3Config.accessKey", "access-key"),
          config("s3Config.secretKey", "secret-key"),
          config("s3Config.signerOverride", "S3SignerType"));

  @Test
  void shouldProvideClient() {
    AmazonS3 client = getClient();

    assertThat(client.getObject("bucket", "key").getObjectContent()).hasContent("data");
  }

  @Test
  void shouldGeneratePresignedUrl() {
    AmazonS3 client = getClient();

    assertThat(client.generatePresignedUrl("bucket", "key", new Date()).toString())
        .contains("Signature=");
  }

  private AmazonS3 getClient() {
    TestApp app = DW.getApplication();
    return app.getS3Bundle().getClient();
  }
}
