package org.sdase.commons.server.kafka.confluent.testing;

import static org.assertj.core.api.Assertions.assertThat;

import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class KafkaBrokerEnvironmentExtensionTest {

  @RegisterExtension
  public static final KafkaBrokerEnvironmentExtension KAFKA =
      new KafkaBrokerEnvironmentExtension(new SharedKafkaTestResource().withBrokers(2));

  @Test
  void shouldBeSetInTest() {

    String compare =
        "[ "
            + KAFKA.getSharedKafkaTestResource().getKafkaBrokers().stream()
                .map(b -> "\"" + b.getConnectString() + "\"")
                .collect(Collectors.joining(", "))
            + " ]";
    assertThat(System.getenv(KafkaBrokerEnvironmentExtension.CONNECTION_STRING_ENV))
        .isEqualTo(compare);
  }
}
