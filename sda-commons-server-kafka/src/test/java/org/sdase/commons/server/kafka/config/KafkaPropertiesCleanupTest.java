package org.sdase.commons.server.kafka.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sdase.commons.server.kafka.KafkaConfiguration;
import org.sdase.commons.server.kafka.KafkaProperties;

class KafkaPropertiesCleanupTest {

  @Test
  void itShouldRemoveBlankValuesForAdminConfig() {
    KafkaConfiguration config = new KafkaConfiguration();

    config.getConfig().put("setting-a", null);
    config.getConfig().put("setting-b", "");
    config.getConfig().put("setting-c", "  ");

    config.getAdminConfig().getConfig().put("admin-setting-a", null);
    config.getAdminConfig().getConfig().put("admin-setting-b", "");
    config.getAdminConfig().getConfig().put("admin-setting-c", "  ");

    assertThat(KafkaProperties.forAdminClient(config))
        .doesNotContainKeys(
            "setting-a",
            "setting-b",
            "setting-c",
            "admin-setting-a",
            "admin-setting-b",
            "admin-setting-c");
  }

  @Test
  void itShouldRemoveBlankValuesForConfig() {
    KafkaConfiguration config = new KafkaConfiguration();

    config.getConfig().put("setting-a", null);
    config.getConfig().put("setting-b", "");
    config.getConfig().put("setting-c", "  ");

    assertThat(KafkaProperties.forProducer(config))
        .doesNotContainKeys("setting-a", "setting-b", "setting-c");
    assertThat(KafkaProperties.forConsumer(config))
        .doesNotContainKeys("setting-a", "setting-b", "setting-c");
  }
}
