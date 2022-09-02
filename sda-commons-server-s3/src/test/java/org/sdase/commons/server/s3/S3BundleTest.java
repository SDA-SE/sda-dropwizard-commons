package org.sdase.commons.server.s3;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.services.s3.AmazonS3;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.s3.test.Config;
import org.sdase.commons.server.s3.testing.S3MockRule;

public class S3BundleTest {

  private static final S3MockRule S_3_MOCK_RULE =
      S3MockRule.builder().putObject("bucket", "key", "data").build();

  @RegisterExtension static final OpenTelemetryExtension OTEL = OpenTelemetryExtension.create();

  private static final DropwizardAppRule<Config> DW =
      new DropwizardAppRule<>(
          TestApp.class,
          null,
          randomPorts(),
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
    List<SpanData> spans = OTEL.getSpans();
    assertThat(spans).extracting(SpanData::getName).contains("S3.GetObject");

    AttributeKey<String> AWS_S3_BUCKET_NAME = stringKey("aws.bucket.name");

    assertThat(
            spans.stream()
                .map(SpanData::getAttributes)
                .map(attributes -> attributes.get(AWS_S3_BUCKET_NAME))
                .collect(Collectors.toList()))
        .isNotEmpty()
        .contains("bucket");
  }

  public static class TestApp extends Application<Config> {
    private S3Bundle<Config> s3Bundle =
        S3Bundle.builder()
            .withConfigurationProvider(Config::getS3Config)
            .withTelemetryInstance(OTEL.getOpenTelemetry())
            .build();

    @Override
    public void initialize(Bootstrap<Config> bootstrap) {
      bootstrap.addBundle(s3Bundle);
    }

    @Override
    public void run(Config configuration, Environment environment) {
      // nothing to run
    }

    public S3Bundle<Config> getS3Bundle() {
      return s3Bundle;
    }
  }
}
