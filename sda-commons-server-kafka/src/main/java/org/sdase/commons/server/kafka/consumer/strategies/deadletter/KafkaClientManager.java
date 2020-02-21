package org.sdase.commons.server.kafka.consumer.strategies.deadletter;

import static org.sdase.commons.server.kafka.consumer.strategies.deadletter.TopicConfigurationHolder.TopicType.*;

import com.github.ftrossbach.club_topicana.core.ExpectedTopicConfiguration;
import io.dropwizard.Configuration;
import java.util.List;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.sdase.commons.server.kafka.KafkaBundle;
import org.sdase.commons.server.kafka.builder.MessageListenerRegistration;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.config.ConsumerConfig;
import org.sdase.commons.server.kafka.config.ProducerConfig;
import org.sdase.commons.server.kafka.consumer.MessageListener;
import org.sdase.commons.server.kafka.consumer.strategies.deadletter.retry.RetryHandler;
import org.sdase.commons.server.kafka.consumer.strategies.synccommit.SyncCommitMLS;
import org.sdase.commons.server.kafka.producer.MessageProducer;

/**
 * The {@link KafkaClientManager} is responsible for the creation of extra {@link MessageProducer}
 * and {@link MessageListener} that are required for a complete {@link DeadLetterMLS} process. The
 * convention how the topics or the configuration names must be named is implemented here. So, the
 * {@link KafkaClientManager} can find the configurations for topics, consumer, and producers or use
 * default settings with only the main topic name
 *
 * <p>Additional Kafka Clients:
 *
 * <ul>
 *   <li>Producer to send messages to the retry topic
 *   <li>Producer to send messages to the dead letter topic
 *   <li>Producer to send messages from retry or dead letter topics to the main topic
 *   <li>Consumer to read messages from retry topic used in {@link
 *       org.sdase.commons.server.kafka.consumer.strategies.deadletter.retry.RetryHandler}
 *   <li>Consumer to read messages from the dead letter topic used in {@link
 *       org.sdase.commons.server.kafka.consumer.strategies.deadletter.dead.DeadLetterTriggerTask}
 * </ul>
 */
public class KafkaClientManager {

  private final KafkaBundle<? extends Configuration> bundle;
  private final TopicConfigurationHolder topicsHolder;

  public KafkaClientManager(
      KafkaBundle<? extends Configuration> bundle, TopicConfigurationHolder topicsHolder) {
    this.bundle = bundle;
    this.topicsHolder = topicsHolder;
  }

  /** @return the producer to insert messages in the retry topic */
  MessageProducer<byte[], byte[]> createMainToRetryTopicProducer() {
    final ExpectedTopicConfiguration retryTopicConfiguration =
        topicsHolder.getTopicConfiguration(RETRY);

    final ProducerConfig retryProducerConfig = createProducerConfig(RETRY, "From-Main-Producer");

    return bundle.registerProducer(
        ProducerRegistration.<byte[], byte[]>builder()
            .forTopic(retryTopicConfiguration)
            .createTopicIfMissing()
            .withProducerConfig(retryProducerConfig)
            .withKeySerializer(new ByteArraySerializer())
            .withValueSerializer(new ByteArraySerializer())
            .build());
  }

  /**
   * Creation of a Kafka Producer to write messages from the dead letter topic to the main topic.
   *
   * @return producer to write messages to main topic
   */
  public MessageProducer<byte[], byte[]> createDeadLetterToMainTopicProducer() {
    return createMainTopicProducer(DEAD_LETTER.toString());
  }

  /**
   * Creation of a Kafka Producer to write messages from the retry topic to the main topic.
   *
   * @return producer to write messages to main topic
   */
  MessageProducer<byte[], byte[]> createRetryToMainTopicProducer() {
    return createMainTopicProducer(RETRY.toString());
  }

  /**
   * creates a producer to insert messages to the main topic
   *
   * @param suffix for the client id to identify producer in metrics
   * @return a message producer that writes messages to the main topic
   */
  private MessageProducer<byte[], byte[]> createMainTopicProducer(String suffix) {
    ExpectedTopicConfiguration topic = topicsHolder.getTopicConfiguration(MAIN);

    final ProducerConfig retryProducerConfig = createProducerConfig(MAIN, suffix);

    return bundle.registerProducer(
        ProducerRegistration.<byte[], byte[]>builder()
            .forTopic(topic)
            .createTopicIfMissing()
            .withProducerConfig(retryProducerConfig)
            .withKeySerializer(new ByteArraySerializer())
            .withValueSerializer(new ByteArraySerializer())
            .build());
  }

  /**
   * When retry mechanism fails, the message must be stored in the dead letter topic for later
   * re-insertion into the main topic. This producer writes the messages to the dead letter topic
   *
   * @return the producer that inserts messages in the dead letter topic
   */
  MessageProducer<byte[], byte[]> createDeadLetterProducer() {

    final ExpectedTopicConfiguration deadLetterTopicConfiguration =
        topicsHolder.getTopicConfiguration(DEAD_LETTER);

    final ProducerConfig deadLetterProducerConfig =
        createProducerConfig(DEAD_LETTER, "-from-retry");

    return bundle.registerProducer(
        ProducerRegistration.<byte[], byte[]>builder()
            .forTopic(deadLetterTopicConfiguration)
            .createTopicIfMissing()
            .withProducerConfig(deadLetterProducerConfig)
            .withKeySerializer(new ByteArraySerializer())
            .withValueSerializer(new ByteArraySerializer())
            .build());
  }

  /**
   * For re-insertion of messages from the retry topic to the main topic, a consumer is required,
   * that reads the messages from the retry topic. The {@link RetryHandler} copies the messages
   * consumed by the created message listener to the target topic. For the re-insertion, the {@link
   * SyncCommitMLS} is used to ensure that all messages are actually written to the target topic.
   * <br>
   * Since the consumer is registered at the bundle, it is stopped when the application stops
   *
   * @param handler Handler that copies consumed messages to the target topic
   * @return retry consumer
   */
  List<MessageListener<byte[], byte[]>> createConsumerForRetryTopic(RetryHandler handler) {
    return bundle.createMessageListener(
        MessageListenerRegistration.<byte[], byte[]>builder()
            .withDefaultListenerConfig()
            .forTopic(topicsHolder.getTopicName(RETRY))
            .withConsumerConfig(createConsumerConfig(RETRY))
            .withKeyDeserializer(new ByteArrayDeserializer())
            .withValueDeserializer(new ByteArrayDeserializer())
            .withListenerStrategy(new SyncCommitMLS<>(handler, handler))
            .build());
  }

  /**
   * Creates a consumer that reads messages from the dead letter task
   *
   * @return consumer
   */
  public KafkaConsumer<byte[], byte[]> createConsumerForDeadLetterTask() {
    return bundle.createConsumer(
        new ByteArrayDeserializer(), // NOSONAR closed when consumer is closed
        new ByteArrayDeserializer(), // NOSONAR closed when consumer is closed
        createConsumerConfig(DEAD_LETTER),
        1);
  }

  /**
   * Creates a default consumer for retry or dead letter topics
   *
   * @param topicType topic type for that the consumer config should be retrieved
   * @return consumer config
   */
  private ConsumerConfig createConsumerConfig(TopicConfigurationHolder.TopicType topicType) {
    return ConsumerConfig.builder()
        .withGroup(topicType.toString())
        .withClientId(topicsHolder.getTopicName(topicType) + "Consumer")
        .build();
  }

  /**
   * Creates a default producer to insert message in the given topic type
   *
   * @param targetTopicType target topic
   * @param nameSuffix additional suffix for the producer to distinguish two producers that write to
   *     the same target topic
   * @return producer config
   */
  private ProducerConfig createProducerConfig(
      TopicConfigurationHolder.TopicType targetTopicType, String nameSuffix) {

    return ProducerConfig.builder()
        .withClientId(
            String.format("%s-%s", topicsHolder.getTopicName(targetTopicType), nameSuffix))
        .build();
  }

  public String getDeadLetterTopicName() {
    return topicsHolder.getTopicName(DEAD_LETTER);
  }
}
