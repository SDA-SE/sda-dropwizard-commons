package org.sdase.commons.server.s3;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.services.s3.AmazonS3;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.Date;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.s3.test.Config;
import org.sdase.commons.server.s3.test.TestApp;
import org.sdase.commons.server.s3.testing.S3MockRule;

public class S3BundleSignerOverrideTest {

  private static final S3MockRule S_3_MOCK_RULE =
      S3MockRule.builder().putObject("bucket", "key", "data").build();

  private static final DropwizardAppRule<Config> DW =
      new DropwizardAppRule<>(
          TestApp.class,
          null,
          randomPorts(),
          config("s3Config.endpoint", S_3_MOCK_RULE::getEndpoint),
          config("s3Config.accessKey", "access-key"),
          config("s3Config.secretKey", "secret-key"),
          config("s3Config.signerOverride", "S3SignerType"));

  @ClassRule public static final RuleChain CHAIN = RuleChain.outerRule(S_3_MOCK_RULE).around(DW);

  @Test()
  public void shouldProvideClient() {
    AmazonS3 client = getClient();

    assertThat(client.getObject("bucket", "key").getObjectContent()).hasContent("data");
  }

  @Test()
  public void shouldGeneratePresignedUrl() {
    AmazonS3 client = getClient();

    assertThat(client.generatePresignedUrl("bucket", "key", new Date()).toString())
        .contains("Signature=");
  }

  private AmazonS3 getClient() {
    TestApp app = DW.getApplication();
    S3Bundle bundle = app.getS3Bundle();
    return bundle.getClient();
  }
}
