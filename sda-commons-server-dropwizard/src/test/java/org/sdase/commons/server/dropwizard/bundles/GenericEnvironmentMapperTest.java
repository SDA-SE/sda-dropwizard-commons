package org.sdase.commons.server.dropwizard.bundles;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.sdase.commons.server.kafka.KafkaConfiguration;
import org.sdase.commons.starter.SdaPlatformConfiguration;

// TODO could be deleted
class GenericEnvironmentMapperTest {
  GenericEnvironmentMapper<CustomConfig> mapper =
      new GenericEnvironmentMapper<>(
          // Note: Using the input (a file path) to mock the content.
          p -> new ByteArrayInputStream(p.getBytes(UTF_8)),
          CustomConfig.class);

  @Test
  @SetSystemProperty(key = "LOGGING_LEVEL", value = "TRACE")
  void shouldSetValue() throws IOException {
    var actual = read(mapper.open("{}"));
    assertThat(actual).isEqualTo(Map.of("logging", Map.of("level", "TRACE")));
  }

  @Test
  void shouldKeepOriginalValue() throws IOException {
    var actual = read(mapper.open("{\"logging\":{\"level\":\"INFO\"}}"));
    assertThat(actual).isEqualTo(Map.of("logging", Map.of("level", "INFO")));
  }

  @Test
  @SetSystemProperty(key = "LOGGING_LEVEL", value = "TRACE")
  void shouldOverwriteValue() throws IOException {
    var actual = read(mapper.open("{\"logging\":{\"level\":\"INFO\"}}"));
    assertThat(actual).isEqualTo(Map.of("logging", Map.of("level", "TRACE")));
  }

  @Test
  @SetSystemProperty(key = "KAFKA_CONFIG", value = "{\"ssl.truststore.type\":\"JKS\"}")
  void shouldMergeMaps() throws IOException {
    var actual = read(mapper.open("{\"kafka\":{\"config\":{\"ssl.keystore.type\":\"JKS\"}}}"));
    assertThat(actual)
        .isEqualTo(
            Map.of(
                "kafka",
                Map.of(
                    "config", Map.of("ssl.keystore.type", "JKS", "ssl.truststore.type", "JKS"))));
  }

  private Object read(InputStream is) throws IOException {
    try (is) {
      return new YAMLMapper().readValue(is, Object.class);
    }
  }

  @SuppressWarnings("unused")
  public static class CustomConfig extends SdaPlatformConfiguration {

    private KafkaConfiguration kafka = new KafkaConfiguration();

    public KafkaConfiguration getKafka() {
      return kafka;
    }

    public CustomConfig setKafka(KafkaConfiguration kafka) {
      this.kafka = kafka;
      return this;
    }
  }
}
