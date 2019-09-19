package org.sdase.commons.server.kafka.consumer.strategies.deadletter;

import static org.sdase.commons.server.kafka.consumer.strategies.deadletter.TopicType.DEAD_LETTER;
import static org.sdase.commons.server.kafka.consumer.strategies.deadletter.TopicType.MAIN;
import static org.sdase.commons.server.kafka.consumer.strategies.deadletter.TopicType.RETRY;

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
import org.sdase.commons.server.kafka.exception.ConfigurationException;
import org.sdase.commons.server.kafka.producer.MessageProducer;

/**
 * The {@link KafkaClientManager} is responsible for the creation of extra {@link MessageProducer}
 * and {@link MessageListener} that are required for a complete {@link DeadLetterMLS} process. The
 * convention how the topics or the configuration names must be named is implemented here. So, the
 * {@link KafkaClientManager} can find the configurations for topics, consumer, and producers or use
 * default settings with only the main topic name
 * <p>
 * Additional Kafka Clients:
 * <ul>
 *   <li>Producer to send messages to the retry topic</li>
 *   <li>Producer to send messages to the dead letter topic</li>
 *   <li>Producer to send messages from retry or dead letter topics to the main topic</li>
 *   <li>Consumer to read messages from retry topic used in {@link org.sdase.commons.server.kafka.consumer.strategies.deadletter.retry.RetryHandler}</li>
 *   <li>Consumer to read messages from the dead letter topic used in {@link org.sdase.commons.server.kafka.consumer.strategies.deadletter.dead.DeadLetterTriggerTask}</li>
 * </ul>
 * </p>
 */
public class KafkaClientManager {

  private final String mainTopicName;
  private final KafkaBundle<? extends Configuration> bundle;
  private final ExpectedTopicConfiguration mainTopicConfiguration;

  public KafkaClientManager(KafkaBundle<? extends Configuration> bundle, String mainTopicName) {
    this.bundle = bundle;
    this.mainTopicConfiguration = bundle.getTopicConfiguration(mainTopicName);
    this.mainTopicName = mainTopicName;
  }

  /**
   * @return the producer to insert messages in the retry topic
   */
  MessageProducer<byte[], byte[]> createMainToRetryTopicProducer() {
    final ExpectedTopicConfiguration retryTopicConfiguration = getOrCreateDefaultTopicConfiguration(
        RETRY);

    final ProducerConfig retryProducerConfig = ProducerConfig.builder()
        .withClientId(retryTopicConfiguration.getTopicName() + "-Producer")
        .build();

    return bundle.registerProducer(ProducerRegistration.<byte[], byte[]>builder()
        .forTopic(retryTopicConfiguration)
        .createTopicIfMissing()
        .checkTopicConfiguration()
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
    final ProducerConfig retryProducerConfig = ProducerConfig.builder()
        .withClientId(String.format("%s - %s", mainTopicName, suffix))
        .build();

    return bundle.registerProducer(ProducerRegistration.<byte[], byte[]>builder()
        .forTopic(mainTopicName)
        .createTopicIfMissing()
        .checkTopicConfiguration()
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
    final ExpectedTopicConfiguration deadLetterTopicConfiguration = getOrCreateDefaultTopicConfiguration(
        DEAD_LETTER);

    final ProducerConfig deadLetterProducerConfig = ProducerConfig.builder()
        .withClientId(deadLetterTopicConfiguration.getTopicName() + "-Producer")
        .build();

    return bundle.registerProducer(ProducerRegistration.<byte[], byte[]>builder()
        .forTopic(deadLetterTopicConfiguration)
        .createTopicIfMissing()
        .checkTopicConfiguration()
        .withProducerConfig(deadLetterProducerConfig)
        .withKeySerializer(new ByteArraySerializer())
        .withValueSerializer(new ByteArraySerializer())
        .build());
  }

  /**
   * For re-insertion of messages from the retry topic to the main topic, a consumer is required,
   * that reads the messages from the retry topic. The {@link RetryHandler} copies the messages
   * consumed by the created message listener to the target topic. For the re-insertion, the
   * {@link SyncCommitMLS} is used to ensure that all messages are actually written to the target
   * topic.
   * <br/>
   * Since the consumer is registered at the bundle, it is stopped when the application stops
   *
   * @param handler Handler that copies consumed  messages to the target topic
   * @return retry consumer
   */
  List<MessageListener<byte[], byte[]>> createConsumerForRetryTopic(RetryHandler handler) {
    return bundle.createMessageListener(MessageListenerRegistration.<byte[], byte[]>builder()
        .withDefaultListenerConfig()
        .forTopic(getOrCreateDefaultTopicConfiguration(RETRY).getTopicName())
        .withConsumerConfig(getOrCreateConsumerConfig(RETRY)
        )
        .withKeyDeserializer(new ByteArrayDeserializer())
        .withValueDeserializer(new ByteArrayDeserializer())
        .withListenerStrategy(new SyncCommitMLS<>(handler, handler))
        .build());
  }

  /**
   * Creates a consumer that reads messages from the dead letter task
   * @return consumer
   */
  public KafkaConsumer<byte[], byte[]> createConsumerForDeadLetterTask() {
    return bundle.createConsumer(
        new ByteArrayDeserializer(), // NOSONAR closed when consumer is closed
        new ByteArrayDeserializer(), // NOSONAR closed when consumer is closed
        getOrCreateConsumerConfig(DEAD_LETTER),
        1);
  }


  /**
   * Returns or calculate the {@link ExpectedTopicConfiguration} for one of the topics in the dead
   * letter strategy, given by convention.
   * <p>
   * If a topic configuration in the {@link org.sdase.commons.server.kafka.KafkaConfiguration} exists
   * this config is returned. Otherwise, a default configuration is created. Partition and replication
   * count is taken from the main topic configuration.
   * </p>
   *
   * @param topicType defines the kind of topic
   * @return TopicConfiguration for the topic type.
   */
  private ExpectedTopicConfiguration getOrCreateDefaultTopicConfiguration(TopicType topicType) {
    try {
      if (topicType == MAIN) {
        return mainTopicConfiguration;
      } else {
        final String configName = mainTopicName + "-" + topicType;
        return bundle.getTopicConfiguration(configName);
      }
    } catch (ConfigurationException e) {
      // No topic configuration has been found in the application config
      // a default one is created based on the main topic configuration
      return new ExpectedTopicConfiguration
          .ExpectedTopicConfigurationBuilder(
          createTopicName(topicType))
          .withPartitionCount(mainTopicConfiguration.getPartitions().count())
          .withReplicationFactor(mainTopicConfiguration.getReplicationFactor().count())
          .build();
    }
  }

  /**
   * Creates a consumer config. If the consumer config is defined in the
   * {@link org.sdase.commons.server.kafka.KafkaConfiguration}, it is taken into account
   * (convention: &lt;mainTopic&gt;-&lt;topicType&gt;, for example 'mainTopic-retry')
   *
   * @param topicType topic type for that the consumer config should be retrieved
   * @return a consumer topic for the given topicType
   */
  private ConsumerConfig getOrCreateConsumerConfig(TopicType topicType) {
    try {
      final String configName = mainTopicName + "-" + topicType.toString();
      return bundle.getConsumerConfiguration(configName);

    } catch (ConfigurationException e) {
      return ConsumerConfig.builder()
          .withGroup(topicType.toString())
          .withClientId(createTopicName(topicType))
          .build();
    }
  }

  /**
   * @return the name of the dead letter topic according to the convention
   */
  public String getDeadLetterTopicName() {
    return getOrCreateDefaultTopicConfiguration(DEAD_LETTER).getTopicName();
  }

  /**
   * Convention for topic naming: retry and dead letter topic must end with suffix
   * '<b>.&lt;topicType&gt;</b>.
   *
   * @param topicType type of the topic for that the name is calculated
   * @return name of the topic defined by the topic type
   */
  private String createTopicName(TopicType topicType) {
    if (topicType == MAIN) {
      return mainTopicName;
    }
    return mainTopicName + "." + topicType.toString();
  }

}
