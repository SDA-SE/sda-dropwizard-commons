package org.sdase.commons.server.kafka.testing;

import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import java.util.List;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Junit 5 extension that will create your Kafka topics. It can be used using {@link
 * org.junit.jupiter.api.extension.RegisterExtension} in your test class. Example:
 *
 * <pre>
 * &#064;RegisterExtension
 * &#064;Order(1)
 * static final CreateKafkaTopicsClassExtension TOPICS =
 *     new CreateKafkaTopicsClassExtension(KAFKA, List.of(TOPIC_NAME, "exampleTopicConfiguration"));
 * </pre>
 *
 * <p>Make sure that this class extension is executed <strong>after</strong> the Kafka server was
 * started but <strong>before</strong> your application starts up.
 */
public class CreateKafkaTopicsClassExtension implements BeforeAllCallback {

  /** Topics to be created. */
  private final List<String> topics;

  /** Helper class to create the topics. */
  private final SharedKafkaTestResource kafka;

  public CreateKafkaTopicsClassExtension(SharedKafkaTestResource kafka, List<String> topics) {
    this.topics = topics;
    this.kafka = kafka;
  }

  /**
   * Creates the given Kafka topics. It will throw a {@link RuntimeException} if topic creation was
   * not successful.
   */
  @Override
  public void beforeAll(ExtensionContext context) {
    topics.forEach(topic -> kafka.getKafkaTestUtils().createTopic(topic, 1, Short.parseShort("1")));
  }
}
