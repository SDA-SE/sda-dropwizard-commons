package org.sdase.commons.server.kafka.confluent.testing;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class KafkaJUnit5IT {

  @RegisterExtension
  @Order(0)
  static SharedKafkaTestResource KAFKA = new SharedKafkaTestResource().withBrokers(2);

  @RegisterExtension
  @Order(1)
  static DropwizardAppExtension<TestConfig> DW =
      new DropwizardAppExtension<>(
          TestApp.class,
          resourceFilePath("test-config.yaml"),
          config("brokers", KAFKA::getKafkaConnectString));

  @Test
  void shouldOverrideBrokers() {
    assertThat(DW.getConfiguration().getBrokers()).isEqualTo(KAFKA.getKafkaConnectString());
  }

  public static class TestApp extends Application<TestConfig> {

    @Override
    public void run(TestConfig configuration, Environment environment) {}
  }

  public static class TestConfig extends Configuration {

    private String brokers;

    public String getBrokers() {
      return brokers;
    }

    @SuppressWarnings("unused")
    public TestConfig setBrokers(String brokers) {
      this.brokers = brokers;
      return this;
    }
  }
}
