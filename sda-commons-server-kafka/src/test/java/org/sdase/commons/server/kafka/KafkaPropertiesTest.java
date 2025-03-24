package org.sdase.commons.server.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KafkaPropertiesTest {

  static final String GIVEN_DW_COMMONS_CONFIG_RESOURCE = "/config/given/kafka-bearer-custom.yaml";
  static final String EXPECTED_KAFKA_CONFIG_RESOURCE = "/config/expected/kafka-config.yaml";

  @Test
  void shouldCreateKafkaPropertiesForOauthBearerAuthentication() {
    var given = readDwCommonsKafkaConfig();
    var actual = KafkaProperties.forAdminClient(given);
    assertThat(actual).isEqualTo(expectedKafkaConfig());
  }

  @Test
  void demonstrateFailureOfDifferentAuthMethodAndUserInfoInSecurity() {
    var given = readDwCommonsKafkaConfig();
    given.getSecurity().setSaslMechanism("OAUTHBEARER");
    given.getSecurity().setUser("some-user");
    given.getSecurity().setPassword("some-password");

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> KafkaProperties.forConsumer(given))
        .withMessageContaining("Unsupported SASL mechanism");
  }

  private KafkaConfiguration readDwCommonsKafkaConfig() {
    return readFromResource(GIVEN_DW_COMMONS_CONFIG_RESOURCE, AppConfigExample.class).kafka();
  }

  private Map<String, Object> expectedKafkaConfig() {
    return readFromResource(EXPECTED_KAFKA_CONFIG_RESOURCE, Expectation.class);
  }

  private <T> T readFromResource(String resource, Class<T> asType) {
    try (var content = getClass().getResourceAsStream(resource)) {
      return new YAMLMapper().readValue(content, asType);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  record AppConfigExample(KafkaConfiguration kafka) {}

  static class Expectation extends HashMap<String, Object> {}
}
