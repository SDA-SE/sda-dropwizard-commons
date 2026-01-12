package org.sdase.commons.server.s3;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.assertj.core.api.Assertions.assertThat;

import com.robothy.s3.jupiter.LocalS3;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.s3.test.Config;
import org.sdase.commons.server.s3.testing.S3ClassExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@LocalS3
class S3BundleTest {

  @RegisterExtension
  @Order(0)
  static final S3ClassExtension S3 =
      S3ClassExtension.builder().putObject("bucket", "key", "data").build();

  @RegisterExtension static final OpenTelemetryExtension OTEL = OpenTelemetryExtension.create();

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<Config> DW =
      new DropwizardAppExtension<>(
          TestApp.class,
          null,
          randomPorts(),
          config("s3Config.endpoint", S3::getEndpoint),
          config("s3Config.accessKey", "access-key"),
          config("s3Config.secretKey", "secret-key"));

  @Test
  void shouldProvideClient() throws IOException {
    TestApp app = DW.getApplication();
    S3Client client = app.getS3Bundle().getClient();

    try (ResponseInputStream<GetObjectResponse> responseInputStream =
        client.getObject(GetObjectRequest.builder().bucket("bucket").key("key").build())) {
      assertThat(responseInputStream.readAllBytes()).asString().isEqualTo("data");
    }
  }

  @Test
  void shouldTraceCalls() throws IOException {
    TestApp app = DW.getApplication();
    S3Client client = app.getS3Bundle().getClient();

    // Create a trace
    try (ResponseInputStream<GetObjectResponse> ignored =
        client.getObject(GetObjectRequest.builder().bucket("bucket").key("key").build())) {
      List<SpanData> spans = OTEL.getSpans();
      assertThat(spans).extracting(SpanData::getName).contains("S3.GetObject");

      AttributeKey<String> awsS3BucketName = stringKey("aws.s3.bucket");

      assertThat(
              spans.stream()
                  .map(SpanData::getAttributes)
                  .map(attributes -> attributes.get(awsS3BucketName))
                  .toList())
          .isNotEmpty()
          .contains("bucket");
    }
  }

  public static class TestApp extends Application<Config> {
    OpenTelemetry openTelemetry = OTEL.getOpenTelemetry();

    private final S3Bundle<Config> s3Bundle =
        S3Bundle.builder()
            .withConfigurationProvider(Config::getS3Config)
            .withOpenTelemetry(openTelemetry)
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
