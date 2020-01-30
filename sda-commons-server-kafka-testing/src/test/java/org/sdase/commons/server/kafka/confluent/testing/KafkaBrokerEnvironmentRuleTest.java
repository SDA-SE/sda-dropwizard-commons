package org.sdase.commons.server.kafka.confluent.testing;

import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.ClassRule;
import org.junit.Test;

public class KafkaBrokerEnvironmentRuleTest {

  private static SharedKafkaTestResource kafkaTestResource =
      new SharedKafkaTestResource().withBrokers(2);

  @ClassRule
  public static KafkaBrokerEnvironmentRule ruleToTest =
      new KafkaBrokerEnvironmentRule(kafkaTestResource);

  @Test
  public void shouldBeSetInTest() {

    String compare =
        "[ "
            + kafkaTestResource.getKafkaBrokers().stream()
                .map(b -> "\"" + b.getConnectString() + "\"")
                .collect(Collectors.joining(", "))
            + " ]";
    Assertions.assertThat(System.getenv(KafkaBrokerEnvironmentRule.CONNECTION_STRING_ENV))
        .isEqualTo(compare);
  }
}
