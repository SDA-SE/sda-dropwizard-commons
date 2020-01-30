package org.sdase.commons.server.s3;

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
import org.sdase.commons.server.testing.DropwizardRuleHelper;
import org.sdase.commons.server.testing.LazyRule;

public class S3BundleSignerOverrideTest {

  private static final S3MockRule S_3_MOCK_RULE =
      S3MockRule.builder().putObject("bucket", "key", "data").build();

  private static final LazyRule<DropwizardAppRule<Config>> DW =
      new LazyRule<>(
          () ->
              DropwizardRuleHelper.dropwizardTestAppFrom(TestApp.class)
                  .withConfigFrom(Config::new)
                  .withRandomPorts()
                  .withConfigurationModifier(
                      c ->
                          c.getS3Config()
                              .setEndpoint(S_3_MOCK_RULE.getEndpoint())
                              .setAccessKey("access-key")
                              .setSecretKey("secret-key")
                              .setSignerOverride("S3SignerType"))
                  .build());

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
    TestApp app = DW.getRule().getApplication();
    S3Bundle bundle = app.getS3Bundle();
    return bundle.getClient();
  }
}
