package org.sdase.commons.server.kafka.testing;

import static org.assertj.core.api.Assertions.assertThat;

import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class CreateKafkaTopicsClassExtensionTest {

  @RegisterExtension
  @Order(0)
  static final SharedKafkaTestResource KAFKA =
      new SharedKafkaTestResource().withBrokerProperty("auto.create.topics.enable", "false");

  @RegisterExtension
  @Order(1)
  static final CreateKafkaTopicsClassExtension TOPICS =
      new CreateKafkaTopicsClassExtension(KAFKA, Arrays.asList("one", "two"));

  @Test
  void shouldCreateTopics() {
    assertThat(KAFKA.getKafkaTestUtils().getTopicNames()).contains("one", "two");
  }

  @Test
  void shouldCreateOneTopic() {
    assertThat(KAFKA.getKafkaTestUtils().getTopicNames()).doesNotContain("three");

    CreateKafkaTopicsClassExtension topics =
        new CreateKafkaTopicsClassExtension(KAFKA, Collections.singletonList("three"));
    topics.beforeAll(null);

    assertThat(KAFKA.getKafkaTestUtils().getTopicNames()).contains("three");
  }
}
