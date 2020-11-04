package org.sdase.commons.server.s3;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.services.s3.AmazonS3;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.s3.test.Config;
import org.sdase.commons.server.s3.test.TestApp;
import org.sdase.commons.server.s3.testing.S3MockRule;

public class S3BundleTest {

  private static final S3MockRule S_3_MOCK_RULE =
      S3MockRule.builder().putObject("bucket", "key", "data").build();

  private static final DropwizardAppRule<Config> DW =
      new DropwizardAppRule<>(
          TestApp.class,
          resourceFilePath("test-config.yaml"),
          config("s3Config.endpoint", S_3_MOCK_RULE::getEndpoint),
          config("s3Config.accessKey", "access-key"),
          config("s3Config.secretKey", "secret-key"));

  @ClassRule public static final RuleChain CHAIN = RuleChain.outerRule(S_3_MOCK_RULE).around(DW);

  @Test()
  public void shouldProvideClient() {
    TestApp app = DW.getApplication();
    S3Bundle bundle = app.getS3Bundle();
    AmazonS3 client = bundle.getClient();

    assertThat(client.getObject("bucket", "key").getObjectContent()).hasContent("data");
  }

  @Test()
  public void shouldTraceCalls() {
    TestApp app = DW.getApplication();
    S3Bundle bundle = app.getS3Bundle();
    AmazonS3 client = bundle.getClient();

    // Create a trace
    client.getObject("bucket", "key").getObjectContent();

    MockTracer tracer = app.getMockTracer();

    assertThat(tracer.finishedSpans())
        .extracting(MockSpan::operationName)
        .contains("GetObjectRequest");
  }
}
