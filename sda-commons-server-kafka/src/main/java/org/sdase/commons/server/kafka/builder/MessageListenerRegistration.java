package org.sdase.commons.server.kafka.builder;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import org.apache.kafka.common.serialization.Deserializer;
import org.sdase.commons.server.kafka.config.ConsumerConfig;
import org.sdase.commons.server.kafka.config.ListenerConfig;
import org.sdase.commons.server.kafka.consumer.strategies.MessageListenerStrategy;
import org.sdase.commons.server.kafka.topicana.ExpectedTopicConfiguration;
import org.sdase.commons.server.kafka.topicana.TopicConfigurationBuilder;

public class MessageListenerRegistration<K, V> {

  private Deserializer<K> keyDeserializer;
  private Deserializer<V> valueDeserializer;
  private Collection<ExpectedTopicConfiguration> topics;
  private boolean checkTopicConfiguration;
  private MessageListenerStrategy<K, V> strategy;

  private ConsumerConfig consumerConfig;
  private String consumerConfigName;
  private String listenerConfigName;
  private ListenerConfig listenerConfig;

  public Deserializer<K> getKeyDeserializer() {
    return keyDeserializer;
  }

  public boolean isCheckTopicConfiguration() {
    return checkTopicConfiguration;
  }

  public String getListenerConfigName() {
    return listenerConfigName;
  }

  public Deserializer<V> getValueDeserializer() {
    return valueDeserializer;
  }

  public Collection<ExpectedTopicConfiguration> getTopics() {
    return topics;
  }

  public Collection<String> getTopicsNames() {
    return topics.stream()
        .map(ExpectedTopicConfiguration::getTopicName)
        .collect(Collectors.toList());
  }

  public MessageListenerStrategy<K, V> getStrategy() {
    return strategy;
  }

  public ConsumerConfig getConsumerConfig() {
    return consumerConfig;
  }

  public String getConsumerConfigName() {
    return consumerConfigName;
  }

  public ListenerConfig getListenerConfig() {
    return listenerConfig;
  }

  public interface ListenerBuilder {

    TopicBuilder withListenerConfig(String name);

    TopicBuilder withListenerConfig(ListenerConfig config);

    TopicBuilder withDefaultListenerConfig();
  }

  public interface TopicBuilder {

    /**
     * @param topic configure the topic to consume
     * @return builder
     */
    ConsumerBuilder forTopic(String topic);

    /**
     * @param topics Collection of topics to consume
     * @return builder
     */
    ConsumerBuilder forTopics(Collection<String> topics);

    /**
     * @param topicConfiguration topic to consume given as topic configuration, e.g. predefined in
     *     config This is necessary if you want to check the topic configuration during startup
     * @return builder
     */
    ConsumerBuilder forTopicConfigs(Collection<ExpectedTopicConfiguration> topicConfiguration);
  }

  public interface ConsumerBuilder {

    /**
     * Define optional step to process a configuration check of the topic. If the topic differs,
     * a @{@link org.sdase.commons.server.kafka.topicana.MismatchedTopicConfigException} will be
     * thrown.
     *
     * @return builder
     */
    ConsumerBuilder checkTopicConfiguration();

    /**
     * @param name name of a consumer config given in the configuration yaml.
     * @return builder
     */
    KeyDeserializerBuilder withConsumerConfig(String name);

    /**
     * @param consumerConfig configuration for a consumer
     * @return builder
     */
    KeyDeserializerBuilder withConsumerConfig(ConsumerConfig consumerConfig);

    /**
     * use the default configuration (1 instance, sync commit...)
     *
     * @return builder
     */
    KeyDeserializerBuilder withDefaultConsumer();
  }

  public interface KeyDeserializerBuilder {

    /**
     * Define key deserializer. This overwrites configuration from ConsumerConfig
     *
     * @param keyDeserializer the serializer
     * @param <K2> the Java type of the message key
     * @return builder
     */
    <K2> ValueDeserializerForDefinedKeyTypeBuilder<K2> withKeyDeserializer(
        Deserializer<K2> keyDeserializer);

    /**
     * Define the value deserializer. This overwrites configuration from ConsumerConfig
     *
     * @param valueDeserializer the serializer
     * @param <V2> the Java type of the message value
     * @return builder
     */
    <V2> ListenerStrategyForDefinedValueTypeBuilder<V2> withValueDeserializer(
        Deserializer<V2> valueDeserializer);

    /**
     * @param strategy the strategy how consumed messages are handled.
     * @param <K2> the Java type of the message key
     * @param <V2> the Java type of the message value
     * @return builder
     */
    <K2, V2> FinalBuilder<K2, V2> withListenerStrategy(MessageListenerStrategy<K2, V2> strategy);
  }

  public interface ListenerStrategyForDefinedValueTypeBuilder<V> {

    /**
     * @param strategy the strategy how consumed messages are handled.
     * @param <K2> The type of the message key
     * @return builder
     */
    <K2> FinalBuilder<K2, V> withListenerStrategy(MessageListenerStrategy<K2, V> strategy);
  }

  public interface ValueDeserializerForDefinedKeyTypeBuilder<K> {

    /**
     * Define the value deserializer. This overwrites configuration from ConsumerConfig
     *
     * @param valueDeserializer the serializer
     * @param <V2> the Java type of the message value
     * @return builder
     */
    <V2> ListenerStrategyBuilder<K, V2> withValueDeserializer(Deserializer<V2> valueDeserializer);

    /**
     * @param strategy the strategy how consumed messages are handled.
     * @param <V2> the type of the message value
     * @return builder
     */
    <V2> FinalBuilder<K, V2> withListenerStrategy(MessageListenerStrategy<K, V2> strategy);
  }

  public interface ListenerStrategyBuilder<K, V> {
    FinalBuilder<K, V> withListenerStrategy(MessageListenerStrategy<K, V> strategy);
  }

  public static ListenerBuilder builder() {
    return new InitialBuilder();
  }

  private static class InitialBuilder
      implements ListenerBuilder, TopicBuilder, ConsumerBuilder, KeyDeserializerBuilder {

    private Collection<ExpectedTopicConfiguration> topics;

    private boolean topicExistCheck = false;
    private ConsumerConfig consumerConfig;
    private ListenerConfig listenerConfig;
    private String consumerName;
    private String listenerName;

    @Override
    public TopicBuilder withListenerConfig(String name) {
      this.listenerName = name;
      return this;
    }

    @Override
    public TopicBuilder withListenerConfig(ListenerConfig config) {
      this.listenerConfig = config;
      return this;
    }

    @Override
    public TopicBuilder withDefaultListenerConfig() {
      this.listenerConfig = ListenerConfig.getDefault();
      return this;
    }

    @Override
    public ConsumerBuilder forTopic(String topic) {
      this.topics = Collections.singletonList(TopicConfigurationBuilder.builder(topic).build());
      return this;
    }

    @Override
    public ConsumerBuilder forTopics(Collection<String> topics) {
      this.topics =
          topics.stream()
              .map(t -> TopicConfigurationBuilder.builder(t).build())
              .collect(Collectors.toList());
      return this;
    }

    @Override
    public ConsumerBuilder forTopicConfigs(
        Collection<ExpectedTopicConfiguration> topicConfiguration) {
      this.topics = topicConfiguration;
      return this;
    }

    @Override
    public ConsumerBuilder checkTopicConfiguration() {
      this.topicExistCheck = true;
      return this;
    }

    @Override
    public KeyDeserializerBuilder withConsumerConfig(String name) {
      this.consumerName = name;
      return this;
    }

    @Override
    public KeyDeserializerBuilder withConsumerConfig(ConsumerConfig consumerConfig) {
      this.consumerConfig = consumerConfig;
      return this;
    }

    @Override
    public KeyDeserializerBuilder withDefaultConsumer() {
      this.consumerConfig = null;
      return this;
    }

    @Override
    public <K2> ValueDeserializerForDefinedKeyTypeBuilder<K2> withKeyDeserializer(
        Deserializer<K2> keyDeserializer) {
      return new PreselectedKeyTypeValueDeserializerBuilder<>(this, keyDeserializer);
    }

    @Override
    public <V2> ListenerStrategyForDefinedValueTypeBuilder<V2> withValueDeserializer(
        Deserializer<V2> valueDeserializer) {
      return new PreselectedValueTypeListenerStrategyBuilder<>(this, valueDeserializer);
    }

    @Override
    public <K2, V2> FinalBuilder<K2, V2> withListenerStrategy(
        MessageListenerStrategy<K2, V2> strategy) {
      return new FinalBuilder<>(this, null, null, strategy);
    }
  }

  private static class PreselectedKeyTypeValueDeserializerBuilder<K>
      implements ValueDeserializerForDefinedKeyTypeBuilder<K> {

    private InitialBuilder initialBuilder;
    private Deserializer<K> keyDeserializer;

    private PreselectedKeyTypeValueDeserializerBuilder(
        InitialBuilder initialBuilder, Deserializer<K> keyDeserializer) {
      this.initialBuilder = initialBuilder;
      this.keyDeserializer = keyDeserializer;
    }

    @Override
    public <V2> ListenerStrategyBuilder<K, V2> withValueDeserializer(
        Deserializer<V2> valueDeserializer) {
      return new PreselectedTypesListenerStrategyBuilder<>(
          initialBuilder, keyDeserializer, valueDeserializer);
    }

    @Override
    public <V2> FinalBuilder<K, V2> withListenerStrategy(MessageListenerStrategy<K, V2> strategy) {
      return new FinalBuilder<>(initialBuilder, keyDeserializer, null, strategy);
    }
  }

  private static class PreselectedValueTypeListenerStrategyBuilder<V>
      implements ListenerStrategyForDefinedValueTypeBuilder<V> {

    private InitialBuilder initialBuilder;
    private Deserializer<V> valueDeserializer;

    private PreselectedValueTypeListenerStrategyBuilder(
        InitialBuilder initialBuilder, Deserializer<V> valueDeserializer) {
      this.initialBuilder = initialBuilder;
      this.valueDeserializer = valueDeserializer;
    }

    @Override
    public <K2> FinalBuilder<K2, V> withListenerStrategy(MessageListenerStrategy<K2, V> strategy) {
      return new FinalBuilder<>(initialBuilder, null, valueDeserializer, strategy);
    }
  }

  private static class PreselectedTypesListenerStrategyBuilder<K, V>
      implements ListenerStrategyBuilder<K, V> {

    private InitialBuilder initialBuilder;
    private Deserializer<K> keyDeserializer;
    private Deserializer<V> valueDeserializer;

    private PreselectedTypesListenerStrategyBuilder(
        InitialBuilder initialBuilder,
        Deserializer<K> keyDeserializer,
        Deserializer<V> valueDeserializer) {
      this.initialBuilder = initialBuilder;
      this.keyDeserializer = keyDeserializer;
      this.valueDeserializer = valueDeserializer;
    }

    @Override
    public FinalBuilder<K, V> withListenerStrategy(MessageListenerStrategy<K, V> strategy) {
      return new FinalBuilder<>(initialBuilder, keyDeserializer, valueDeserializer, strategy);
    }
  }

  public static class FinalBuilder<K, V> {

    private InitialBuilder initialBuilder;
    private Deserializer<K> keyDeserializer;
    private Deserializer<V> valueDeserializer;
    private MessageListenerStrategy<K, V> messageListenerStrategy;

    private FinalBuilder(
        InitialBuilder initialBuilder,
        Deserializer<K> keyDeserializer,
        Deserializer<V> valueDeserializer,
        MessageListenerStrategy<K, V> messageListenerStrategy) {
      this.initialBuilder = initialBuilder;
      this.keyDeserializer = keyDeserializer;
      this.valueDeserializer = valueDeserializer;
      this.messageListenerStrategy = messageListenerStrategy;
    }

    public MessageListenerRegistration<K, V> build() {
      MessageListenerRegistration<K, V> build = new MessageListenerRegistration<>();

      build.keyDeserializer = keyDeserializer;
      build.valueDeserializer = valueDeserializer;
      build.topics = initialBuilder.topics;
      build.checkTopicConfiguration = initialBuilder.topicExistCheck;
      build.consumerConfig = initialBuilder.consumerConfig;
      build.consumerConfigName = initialBuilder.consumerName;
      build.listenerConfig = initialBuilder.listenerConfig;
      build.listenerConfigName = initialBuilder.listenerName;
      build.strategy = messageListenerStrategy;

      return build;
    }
  }
}
