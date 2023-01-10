package org.sdase.commons.server.s3;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.services.s3.AmazonS3;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.s3.test.Config;
import org.sdase.commons.server.s3.testing.S3ClassExtension;

class S3BundleTest {

  @RegisterExtension
  @Order(0)
  private static final S3ClassExtension S3 =
      S3ClassExtension.builder().putObject("bucket", "key", "data").build();

  @RegisterExtension static final OpenTelemetryExtension OTEL = OpenTelemetryExtension.create();

  @RegisterExtension
  @Order(1)
  private static final DropwizardAppExtension<Config> DW =
      new DropwizardAppExtension<>(
          TestApp.class,
          null,
          randomPorts(),
          config("s3Config.endpoint", S3::getEndpoint),
          config("s3Config.accessKey", "access-key"),
          config("s3Config.secretKey", "secret-key"));

  @Test
  void shouldProvideClient() {
    TestApp app = DW.getApplication();
    AmazonS3 client = app.getS3Bundle().getClient();

    assertThat(client.getObject("bucket", "key").getObjectContent()).hasContent("data");
  }

  @Test
  void shouldTraceCalls() {
    TestApp app = DW.getApplication();
    AmazonS3 client = app.getS3Bundle().getClient();

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
    OpenTelemetry openTelemetry = OTEL.getOpenTelemetry();

    private S3Bundle<Config> s3Bundle =
        S3Bundle.builder()
            .withConfigurationProvider(Config::getS3Config)
            .withOpenTelemetry(OTEL.getOpenTelemetry())
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
