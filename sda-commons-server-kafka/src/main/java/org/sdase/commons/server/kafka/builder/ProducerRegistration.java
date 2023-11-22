package org.sdase.commons.server.kafka.builder;

import jakarta.validation.constraints.NotNull;
import org.apache.kafka.common.serialization.Serializer;
import org.sdase.commons.server.kafka.config.ProducerConfig;
import org.sdase.commons.server.kafka.config.TopicConfig;

public class ProducerRegistration<K, V> {

  private Serializer<K> keySerializer;
  private Serializer<V> valueSerializer;
  private TopicConfig topic;
  private ProducerConfig producerConfig;
  private String producerName;

  public String getProducerConfigName() {
    return producerName;
  }

  public TopicConfig getTopic() {
    return this.topic;
  }

  public String getTopicName() {
    return topic.getName();
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
      return forTopic(TopicConfig.builder().name(topic).build());
    }

    /**
     * @param topic definition of the topic for that messages will be produced
     * @return builder
     */
    ProducerBuilder<K, V> forTopic(TopicConfig topic);
  }

  public interface ProducerBuilder<K, V> {

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

    private final InitialBuilder<K, V> initialBuilder;
    private final Serializer<K> keySerializer;

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

    private final InitialBuilder<K, V> initialBuilder;
    private final Serializer<K> keySerializer;
    private final Serializer<V> valueSerializer;

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

    private TopicConfig topic;
    private ProducerConfig producerConfig;
    private String producerName = null;

    private InitialBuilder() {}

    static <K, V, K2, V2> InitialBuilder<K2, V2> clone(InitialBuilder<K, V> source) {
      InitialBuilder<K2, V2> target = new InitialBuilder<>();
      target.topic = source.topic;
      target.producerConfig = source.producerConfig;
      target.producerName = source.producerName;
      return target;
    }

    @Override
    public ProducerBuilder<K, V> forTopic(@NotNull TopicConfig topic) {
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
    build.producerConfig = initialBuilder.producerConfig;
    build.producerName = initialBuilder.producerName;

    return build;
  }
}
