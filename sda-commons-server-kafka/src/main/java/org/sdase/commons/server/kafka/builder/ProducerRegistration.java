package org.sdase.commons.server.kafka.builder;

import javax.validation.constraints.NotNull;
import org.apache.kafka.common.serialization.Serializer;
import org.sdase.commons.server.kafka.config.ProducerConfig;
import org.sdase.commons.server.kafka.exception.TopicMissingException;
import org.sdase.commons.server.kafka.topicana.ExpectedTopicConfiguration;
import org.sdase.commons.server.kafka.topicana.TopicConfigurationBuilder;

public class ProducerRegistration<K, V> {

  private Serializer<K> keySerializer;
  private Serializer<V> valueSerializer;
  private ExpectedTopicConfiguration topic;

  private boolean checkTopicConfiguration;
  private ProducerConfig producerConfig;
  private String producerName;
  private boolean createTopicIfMissing;

  public String getProducerConfigName() {
    return producerName;
  }

  public ExpectedTopicConfiguration getTopic() {
    return topic;
  }

  public String getTopicName() {
    return topic.getTopicName();
  }

  public boolean isCheckTopicConfiguration() {
    return checkTopicConfiguration;
  }

  public boolean isCreateTopicIfMissing() {
    return createTopicIfMissing;
  }

  public Serializer<K> getKeySerializer() {
    return keySerializer;
  }

  public Serializer<V> getValueSerializer() {
    return valueSerializer;
  }

  public ProducerConfig getProducerConfig() {
    return producerConfig;
  }

  public interface TopicBuilder<K, V> {

    /**
     * @param topic the topic for that messages will be produced
     * @return builder
     */
    default ProducerBuilder<K, V> forTopic(String topic) {
      return forTopic(TopicConfigurationBuilder.builder(topic).build());
    }

    /**
     * @param topic detailed definition of the topic for that messages will be produced. This
     *     details are used when topic existence should be checked or topic should be created if
     *     missing. If topic differs, a {@link TopicMissingException} is thrown.
     * @return builder
     */
    ProducerBuilder<K, V> forTopic(ExpectedTopicConfiguration topic);
  }

  public interface ProducerBuilder<K, V> {

    /**
     * defines that the topic configuration should be checked. If the name is given only, just topic
     * existence is checked.
     *
     * @return builder
     */
    ProducerBuilder<K, V> checkTopicConfiguration();

    /**
     * defines that the topic should be created if it does not exist
     *
     * @return builder
     */
    ProducerBuilder<K, V> createTopicIfMissing();

    /**
     * defines that the default producer should be used
     *
     * @return builder
     */
    KeySerializerBuilder<K, V> withDefaultProducer();

    /**
     * @param config configuration used for KafkaProducer creation
     * @return builder
     */
    KeySerializerBuilder<K, V> withProducerConfig(ProducerConfig config);

    /**
     * @param name name of the ProducerConfig that is defined in the coniguration yaml
     * @return builder
     */
    KeySerializerBuilder<K, V> withProducerConfig(String name);
  }

  public interface KeySerializerBuilder<K, V> {

    /**
     * @param keySerializer define a new key serializer
     * @param <K2> the Java type of the message key
     * @return builder
     */
    <K2> ValueSerializerBuilder<K2, V> withKeySerializer(Serializer<K2> keySerializer);

    /**
     * @param valueSerializer define a new value serializer
     * @param <V2> the Java type of the message value
     * @return builder
     */
    <V2> FinalBuilder<K, V2> withValueSerializer(Serializer<V2> valueSerializer);

    ProducerRegistration<K, V> build();
  }

  public static class ValueSerializerBuilder<K, V> {

    private InitialBuilder<K, V> initialBuilder;
    private Serializer<K> keySerializer;

    private ValueSerializerBuilder(
        InitialBuilder<K, V> initialBuilder, Serializer<K> keySerializer) {
      this.initialBuilder = initialBuilder;
      this.keySerializer = keySerializer;
    }

    /**
     * @param valueSerializer define a new value serializer
     * @param <V2> the Java type of the message value
     * @return builder
     */
    public <V2> FinalBuilder<K, V2> withValueSerializer(Serializer<V2> valueSerializer) {
      return new FinalBuilder<>(
          InitialBuilder.clone(initialBuilder), keySerializer, valueSerializer);
    }

    public ProducerRegistration<K, V> build() {
      return ProducerRegistration.build(initialBuilder, keySerializer, null);
    }
  }

  public static class FinalBuilder<K, V> {

    private InitialBuilder<K, V> initialBuilder;
    private Serializer<K> keySerializer;
    private Serializer<V> valueSerializer;

    private FinalBuilder(
        InitialBuilder<K, V> initialBuilder,
        Serializer<K> keySerializer,
        Serializer<V> valueSerializer) {
      this.initialBuilder = initialBuilder;
      this.keySerializer = keySerializer;
      this.valueSerializer = valueSerializer;
    }

    public ProducerRegistration<K, V> build() {
      return ProducerRegistration.build(initialBuilder, keySerializer, valueSerializer);
    }
  }

  /**
   * creates a new builder
   *
   * @param <K> the Java type of the message key
   * @param <V> the Java type of the message value
   * @return builder
   */
  public static <K, V> TopicBuilder<K, V> builder() {
    return new InitialBuilder<>();
  }

  private static class InitialBuilder<K, V>
      implements TopicBuilder<K, V>, ProducerBuilder<K, V>, KeySerializerBuilder<K, V> {

    private ExpectedTopicConfiguration topic;
    private boolean checkTopicConfiguration = false;
    private boolean createTopicIfMissing = false;
    private ProducerConfig producerConfig;
    private String producerName = null;

    private InitialBuilder() {}

    static <K, V, K2, V2> InitialBuilder<K2, V2> clone(InitialBuilder<K, V> source) {
      InitialBuilder<K2, V2> target = new InitialBuilder<>();
      target.checkTopicConfiguration = source.checkTopicConfiguration;
      target.createTopicIfMissing = source.createTopicIfMissing;
      target.topic = source.topic;
      target.producerConfig = source.producerConfig;
      target.producerName = source.producerName;
      return target;
    }

    @Override
    public ProducerBuilder<K, V> forTopic(@NotNull ExpectedTopicConfiguration topic) {
      this.topic = topic;
      return this;
    }

    @Override
    public <K1> ValueSerializerBuilder<K1, V> withKeySerializer(Serializer<K1> keySerializer) {
      return new ValueSerializerBuilder<>(InitialBuilder.clone(this), keySerializer);
    }

    @Override
    public <V1> FinalBuilder<K, V1> withValueSerializer(Serializer<V1> valueSerializer) {
      return new FinalBuilder<>(InitialBuilder.clone(this), null, valueSerializer);
    }

    @Override
    public ProducerBuilder<K, V> checkTopicConfiguration() {
      this.checkTopicConfiguration = true;
      return this;
    }

    @Override
    public ProducerBuilder<K, V> createTopicIfMissing() {
      this.createTopicIfMissing = true;
      return this;
    }

    @Override
    public KeySerializerBuilder<K, V> withDefaultProducer() {
      this.producerConfig = null;
      this.producerName = null;
      return this;
    }

    @Override
    public KeySerializerBuilder<K, V> withProducerConfig(ProducerConfig config) {
      this.producerConfig = config;
      return this;
    }

    @Override
    public KeySerializerBuilder<K, V> withProducerConfig(String name) {
      this.producerName = name;
      return this;
    }

    @Override
    public ProducerRegistration<K, V> build() {
      return ProducerRegistration.build(this, null, null);
    }
  }

  private static <K, V> ProducerRegistration<K, V> build(
      InitialBuilder<K, V> initialBuilder,
      Serializer<K> keySerializer,
      Serializer<V> valueSerializer) {
    ProducerRegistration<K, V> build = new ProducerRegistration<>();
    build.keySerializer = keySerializer;
    build.valueSerializer = valueSerializer;
    build.topic = initialBuilder.topic;
    build.checkTopicConfiguration = initialBuilder.checkTopicConfiguration;
    build.createTopicIfMissing = initialBuilder.createTopicIfMissing;
    build.producerConfig = initialBuilder.producerConfig;
    build.producerName = initialBuilder.producerName;

    return build;
  }
}
