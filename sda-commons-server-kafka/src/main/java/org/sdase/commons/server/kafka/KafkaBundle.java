package org.sdase.commons.server.kafka;

import static org.sdase.commons.server.dropwizard.lifecycle.ManagedShutdownListener.onShutdown;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.sdase.commons.server.kafka.builder.MessageListenerRegistration;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.config.ConsumerConfig;
import org.sdase.commons.server.kafka.config.ListenerConfig;
import org.sdase.commons.server.kafka.config.ProducerConfig;
import org.sdase.commons.server.kafka.config.TopicConfig;
import org.sdase.commons.server.kafka.consumer.MessageListener;
import org.sdase.commons.server.kafka.exception.ConfigurationException;
import org.sdase.commons.server.kafka.exception.TopicCreationException;
import org.sdase.commons.server.kafka.health.ExternalKafkaHealthCheck;
import org.sdase.commons.server.kafka.health.KafkaHealthCheck;
import org.sdase.commons.server.kafka.producer.KafkaMessageProducer;
import org.sdase.commons.server.kafka.producer.MessageProducer;
import org.sdase.commons.server.kafka.prometheus.ConsumerTopicMessageHistogram;
import org.sdase.commons.server.kafka.prometheus.KafkaConsumerMetrics;
import org.sdase.commons.server.kafka.prometheus.ProducerTopicMessageCounter;
import org.sdase.commons.server.kafka.topicana.ComparisonResult;
import org.sdase.commons.server.kafka.topicana.EvaluationException;
import org.sdase.commons.server.kafka.topicana.ExpectedTopicConfiguration;
import org.sdase.commons.server.kafka.topicana.MismatchedTopicConfigException;
import org.sdase.commons.server.kafka.topicana.TopicComparer;
import org.sdase.commons.server.kafka.topicana.TopicConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("WeakerAccess")
public class KafkaBundle<C extends Configuration> implements ConfiguredBundle<C> {

  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaBundle.class);

  public static final String HEALTHCHECK_NAME = "kafkaConnection";
  public static final String EXTERNAL_HEALTHCHECK_NAME = "kafkaConnectionExternal";

  private final Function<C, KafkaConfiguration> configurationProvider;
  private KafkaConfiguration kafkaConfiguration;
  private boolean healthCheckDisabled;

  private ProducerTopicMessageCounter topicProducerCounterSpec;
  private ConsumerTopicMessageHistogram topicConsumerHistogram;

  private List<MessageListener<?, ?>> messageListeners = new ArrayList<>();
  private List<ThreadedMessageListener<?, ?>> threadedMessageListeners = new ArrayList<>();
  private List<KafkaMessageProducer<?, ?>> messageProducers = new ArrayList<>();

  private Map<String, ExpectedTopicConfiguration> topics = new HashMap<>();

  private KafkaBundle(
      KafkaConfigurationProvider<C> configurationProvider, boolean healthCheckDisabled) {
    this.configurationProvider = configurationProvider;
    this.healthCheckDisabled = healthCheckDisabled;
  }

  public static InitialBuilder builder() {
    return new Builder<>();
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    //
  }

  @SuppressWarnings("unused")
  @Override
  public void run(C configuration, Environment environment) {
    kafkaConfiguration = configurationProvider.apply(configuration);
    kafkaConfiguration.getTopics().forEach((k, v) -> topics.put(k, createTopicDescription(v)));
    if (!kafkaConfiguration.isDisabled()) {
      if (healthCheckDisabled) {
        environment
            .healthChecks()
            .register(EXTERNAL_HEALTHCHECK_NAME, new ExternalKafkaHealthCheck(kafkaConfiguration));
      } else {
        environment
            .healthChecks()
            .register(HEALTHCHECK_NAME, new KafkaHealthCheck(kafkaConfiguration));
      }
    }
    topicProducerCounterSpec = new ProducerTopicMessageCounter();
    topicConsumerHistogram = new ConsumerTopicMessageHistogram();
    new KafkaConsumerMetrics(messageListeners);
    setupManagedThreadManager(environment);
  }

  /**
   * Provides a {@link ExpectedTopicConfiguration} that is generated from the values within the
   * configuration yaml
   *
   * @param name the name of the topic
   * @return the configured topic configuration
   * @throws ConfigurationException if no such topic exists in the configuration
   */
  public ExpectedTopicConfiguration getTopicConfiguration(String name)
      throws ConfigurationException { // NOSONAR
    if (topics.get(name) == null) {
      throw new ConfigurationException(
          String.format(
              "Topic with name '%s' seems not to be part of the read configuration. Please check the name and configuration.",
              name));
    }
    return topics.get(name);
  }

  /**
   * Creates a number of message listeners with the parameters given in the {@link
   * MessageListenerRegistration}.
   *
   * <p>The depricated fields of the {@link ListenerConfig} from {@link MessageListenerRegistration}
   * are not considered here
   *
   * @param registration the registration configuration
   * @param <K> the key object type
   * @param <V> the value object type
   * @return the newly registered message listeners
   */
  public <K, V> List<MessageListener<K, V>> createMessageListener(
      MessageListenerRegistration<K, V> registration) {
    if (kafkaConfiguration.isDisabled()) {
      return Collections.emptyList();
    }

    checkInit();

    if (registration.isCheckTopicConfiguration()) {
      ComparisonResult comparisonResult = checkTopics(registration.getTopics());
      if (!comparisonResult.ok()) {
        throw new MismatchedTopicConfigException(comparisonResult);
      }
    }

    ListenerConfig listenerConfig = registration.getListenerConfig();
    if (listenerConfig == null && registration.getListenerConfigName() != null) {
      listenerConfig =
          kafkaConfiguration.getListenerConfig().get(registration.getListenerConfigName());
      if (listenerConfig == null) {
        throw new ConfigurationException(
            String.format(
                "Listener config with name '%s' cannot be found within the current configuration.",
                registration.getListenerConfigName()));
      }
    }

    if (listenerConfig == null) {
      throw new ConfigurationException(
          "No valid listener config given within the MessageHandlerRegistration");
    }

    if (registration.getStrategy() == null) {
      throw new IllegalStateException("A strategy is mandatory for message listeners.");
    }

    List<MessageListener<K, V>> listener = new ArrayList<>(listenerConfig.getInstances());
    for (int i = 0; i < listenerConfig.getInstances(); i++) {
      registration.getStrategy().init(topicConsumerHistogram);
      MessageListener<K, V> instance =
          new MessageListener<>(
              registration.getTopicsNames(),
              createConsumer(registration, i),
              listenerConfig,
              registration.getStrategy());

      listener.add(instance);
      Thread t = new Thread(instance);
      t.start();

      threadedMessageListeners.add(new ThreadedMessageListener<>(instance, t));
    }
    messageListeners.addAll(listener);
    return listener;
  }

  /**
   * creates a @{@link MessageProducer} based on the data in the {@link ProducerRegistration}
   *
   * <p>if the kafka bundle is disabled, null is returned
   *
   * @param registration the configuration object
   * @param <K> key clazz type
   * @param <V> value clazz type
   * @return message producer
   * @throws ConfigurationException if the {@code registration} has no {@link
   *     ProducerRegistration#getProducerConfig()} and there is no configuration available with the
   *     same name as defined in {@link ProducerRegistration#getProducerConfigName()}
   */
  public <K, V> MessageProducer<K, V> registerProducer(ProducerRegistration<K, V> registration)
      throws ConfigurationException { // NOSONAR

    // if Kafka is disabled (for testing issues), we return a dummy producer
    // only.
    // This dummy works as long as the future is not evaluated
    if (kafkaConfiguration.isDisabled()) {
      return new MessageProducer<K, V>() {
        @Override
        public Future<RecordMetadata> send(K key, V value) {
          return null;
        }

        @Override
        public Future<RecordMetadata> send(K key, V value, Headers headers) {
          return null;
        }
      };
    }

    checkInit();

    if (registration.isCreateTopicIfMissing()) {
      createNotExistingTopics(Collections.singletonList(registration.getTopic()));
    }

    if (registration.isCheckTopicConfiguration()) {
      ComparisonResult comparisonResult =
          checkTopics(Collections.singletonList(registration.getTopic()));
      if (!comparisonResult.ok()) {
        throw new MismatchedTopicConfigException(comparisonResult);
      }
    }
    KafkaProducer<K, V> producer = createProducer(registration);
    Entry<MetricName, ? extends Metric> entry =
        producer.metrics().entrySet().stream().findFirst().orElse(null);
    String clientId = entry != null ? entry.getKey().tags().get("client-id") : "";

    KafkaMessageProducer<K, V> messageProducer =
        new KafkaMessageProducer<>(
            registration.getTopicName(), producer, topicProducerCounterSpec, clientId);

    messageProducers.add(messageProducer);
    return messageProducer;
  }

  /**
   * Checks or creates a collection of topics with respect to its configuration
   *
   * @param topics Collection of topic configurations that should be checked
   */
  private void createNotExistingTopics(Collection<ExpectedTopicConfiguration> topics) {
    if (kafkaConfiguration.isDisabled()) {
      return;
    }
    // find out what topics are missing
    ComparisonResult comparisonResult = checkTopics(topics);
    if (!comparisonResult.getMissingTopics().isEmpty()) {
      createTopics(
          topics.stream()
              .filter(t -> comparisonResult.getMissingTopics().contains(t.getTopicName()))
              .collect(Collectors.toList()));
    }
  }

  /**
   * Checks if the defined topics are available on the broker for the provided credentials
   *
   * @param topics list of topics to test
   */
  private ComparisonResult checkTopics(Collection<ExpectedTopicConfiguration> topics) {
    if (kafkaConfiguration.isDisabled()) {
      return new ComparisonResult.ComparisonResultBuilder().build();
    }
    TopicComparer topicComparer = new TopicComparer();
    return topicComparer.compare(topics, kafkaConfiguration);
  }

  private void createTopics(Collection<ExpectedTopicConfiguration> topics) {
    try (final AdminClient adminClient =
        AdminClient.create(KafkaProperties.forAdminClient(kafkaConfiguration))) {
      List<NewTopic> topicList =
          topics.stream()
              .map(
                  t -> {
                    int partitions = 1;
                    int replications = 1;
                    if (!t.getPartitions().isSpecified()) {
                      LOGGER.warn(
                          "Partitions for topic '{}' is not specified. Using default value 1",
                          t.getTopicName());
                    } else {
                      partitions = t.getPartitions().count();
                    }

                    if (!t.getReplicationFactor().isSpecified()) {
                      LOGGER.warn(
                          "Replication factor for topic '{}' is not specified. Using default value 1",
                          t.getTopicName());
                    } else {
                      replications = t.getReplicationFactor().count();
                    }

                    return new NewTopic(t.getTopicName(), partitions, (short) replications)
                        .configs(t.getProps());
                  })
              .collect(Collectors.toList());
      CreateTopicsResult result = adminClient.createTopics(topicList);
      result.all().get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new EvaluationException("Exception during adminClient.createTopics", e);
    } catch (ExecutionException e) {
      throw new TopicCreationException("TopicConfig creation failed", e);
    }
  }

  @SuppressWarnings("static-method")
  private ExpectedTopicConfiguration createTopicDescription(TopicConfig c) {
    return TopicConfigurationBuilder.builder(c.getName())
        .withPartitionCount(c.getPartitions())
        .withReplicationFactor(c.getReplicationFactor())
        .withConfig(c.getConfig())
        .build();
  }

  /**
   * creates a new Kafka Consumer with deserializers and consumer config
   *
   * <p>Note: after creating a {@code KafkaConsumer} you must always {@link KafkaConsumer#close()}
   * it to avoid resource leaks.
   *
   * @param keyDeSerializer deserializer for key objects. If null, value from config or default
   *     {@link org.apache.kafka.common.serialization.StringDeserializer} will be used
   * @param valueDeSerializer deserializer for value objects. If null, value from config or *
   *     default * {@link org.apache.kafka.common.serialization.StringDeserializer} * will be used
   * @param consumerConfigName name of a valid consumer config
   * @param <K> Key object type
   * @param <V> Value object type
   * @return a new kafka consumer
   */
  public <K, V> KafkaConsumer<K, V> createConsumer(
      Deserializer<K> keyDeSerializer,
      Deserializer<V> valueDeSerializer,
      String consumerConfigName) {

    ConsumerConfig consumerConfig = getConsumerConfiguration(consumerConfigName);
    return createConsumer(keyDeSerializer, valueDeSerializer, consumerConfig, 0);
  }

  /**
   * creates a new Kafka Consumer with deserializers and consumer config
   *
   * <p>Note: after creating a {@code KafkaConsumer} you must always {@link KafkaConsumer#close()}
   * it to avoid resource leaks.
   *
   * @param keyDeSerializer deserializer for key objects. If null, value from config or default
   *     {@link org.apache.kafka.common.serialization.StringDeserializer} will be used
   * @param valueDeSerializer deserializer for value objects. If null, value from config or *
   *     default * {@link org.apache.kafka.common.serialization.StringDeserializer} * will be used
   * @param consumerConfig config of the consumer. If null a consumer with default config values is
   *     created.
   * @param instanceId the id of the consumer that is appended to the client ID
   * @param <K> Key object type
   * @param <V> Value object type
   * @return a new kafka consumer
   */
  public <K, V> KafkaConsumer<K, V> createConsumer(
      Deserializer<K> keyDeSerializer,
      Deserializer<V> valueDeSerializer,
      ConsumerConfig consumerConfig,
      int instanceId) {
    KafkaProperties consumerProperties = KafkaProperties.forConsumer(kafkaConfiguration);
    if (consumerConfig != null) {
      consumerProperties.putAll(consumerConfig.getConfig());
      consumerProperties.put(
          org.apache.kafka.clients.consumer.ConsumerConfig.CLIENT_ID_CONFIG,
          consumerConfig.getClientId() + "-" + instanceId);
    }

    return new KafkaConsumer<>(consumerProperties, keyDeSerializer, valueDeSerializer);
  }

  /**
   * creates a new Kafka Consumer with deserializers and consumer config
   *
   * <p>Note: after creating a {@code KafkaProducer} you must always {@link KafkaProducer#close()}
   * it to avoid resource leaks.
   *
   * @param keySerializer deserializer for key objects. If null, value from config or default {@link
   *     org.apache.kafka.common.serialization.StringSerializer} will be used
   * @param valueSerializer deserializer for value objects. If null, value from config or * default
   *     * {@link org.apache.kafka.common.serialization.StringSerializer} * will be used
   * @param producerConfig config of the producer
   * @param <K> Key object type
   * @param <V> Value object type
   * @return a new kafka producer
   */
  public <K, V> KafkaProducer<K, V> createProducer(
      Serializer<K> keySerializer, Serializer<V> valueSerializer, ProducerConfig producerConfig) {
    KafkaProperties producerProperties = KafkaProperties.forProducer(kafkaConfiguration);

    if (producerConfig != null) {
      producerConfig.getConfig().forEach(producerProperties::put);
    }

    return new KafkaProducer<>(producerProperties, keySerializer, valueSerializer);
  }

  /**
   * creates a new Kafka Consumer with deserializers and consumer config
   *
   * <p>Note: after creating a {@code KafkaProducer} you must always {@link KafkaProducer#close()}
   * it to avoid resource leaks.
   *
   * @param keySerializer deserializer for key objects. If null, value from config or default {@link
   *     org.apache.kafka.common.serialization.StringSerializer} will be used
   * @param valueSerializer deserializer for value objects. If null, value from config or * default
   *     * {@link org.apache.kafka.common.serialization.StringSerializer} * will be used
   * @param producerConfigName name of the producer config to be used
   * @param <K> Key object type
   * @param <V> Value object type
   * @return a new kafka producer
   */
  public <K, V> KafkaProducer<K, V> createProducer(
      Serializer<K> keySerializer, Serializer<V> valueSerializer, String producerConfigName) {

    ProducerConfig producerConfig = getProducerConfiguration(producerConfigName);
    if (producerConfig != null && producerConfig.getClientId() == null) {
      producerConfig.setClientId(producerConfigName);
    }

    return createProducer(keySerializer, valueSerializer, producerConfig);
  }

  private <K, V> KafkaProducer<K, V> createProducer(ProducerRegistration<K, V> registration) {

    ProducerConfig producerConfig = registration.getProducerConfig();
    if (producerConfig == null && registration.getProducerConfigName() != null) {
      producerConfig = getProducerConfiguration(registration.getProducerConfigName());
    }
    if (producerConfig != null && producerConfig.getClientId() == null) {
      producerConfig.setClientId(registration.getProducerConfigName());
    }

    return createProducer(
        registration.getKeySerializer(), registration.getValueSerializer(), producerConfig);
  }

  /**
   * returns a @{@link ConsumerConfig} as defined in the configuration
   *
   * @param name name of the consumer in the configuration
   * @return Consumer Configuration
   * @throws ConfigurationException exception of the configuration does not exist
   */
  public ConsumerConfig getConsumerConfiguration(String name) {
    if (!kafkaConfiguration.getConsumers().containsKey(name)) {
      throw new ConfigurationException(
          String.format(
              "Consumer config with name '%s' cannot be found within the current configuration.",
              name));
    }
    return kafkaConfiguration.getConsumers().get(name);
  }

  /**
   * returns a @{@link ProducerConfig} as defined in the configuration
   *
   * @param name name of the producer in the configuration
   * @return Producer Configuration
   * @throws ConfigurationException exception of the configuration does not exist
   */
  public ProducerConfig getProducerConfiguration(String name) {
    if (!kafkaConfiguration.getProducers().containsKey(name)) {
      throw new ConfigurationException(
          String.format(
              "Producer config with name '%s' cannot be found within the current configuration.",
              name));
    }
    return kafkaConfiguration.getProducers().get(name);
  }

  private <K, V> KafkaConsumer<K, V> createConsumer(
      MessageListenerRegistration<K, V> registration, int instanceId) {
    ConsumerConfig consumerConfig = registration.getConsumerConfig();
    if (consumerConfig == null && registration.getConsumerConfigName() != null) {
      consumerConfig = getConsumerConfiguration(registration.getConsumerConfigName());
    }
    if (consumerConfig != null) {
      applyForcedConfigFromStrategy(registration, consumerConfig);
      registration.getStrategy().verifyConsumerConfig(consumerConfig.getConfig());
    }
    if (consumerConfig != null && consumerConfig.getClientId() == null) {
      consumerConfig.setClientId(registration.getConsumerConfigName());
    }
    return createConsumer(
        registration.getKeyDeserializer(),
        registration.getValueDeserializer(),
        consumerConfig,
        instanceId);
  }

  private void applyForcedConfigFromStrategy(
      MessageListenerRegistration<?, ?> registration, ConsumerConfig consumerConfig) {
    Map<String, String> newConfig = new HashMap<>(consumerConfig.getConfig());
    Map<String, String> forcedConfig = registration.getStrategy().forcedConfigToApply();
    for (Entry<String, String> configEntry : forcedConfig.entrySet()) {
      String key = configEntry.getKey();
      String newValue = configEntry.getValue();
      String oldValue =
          newConfig.get(key); // NOSONAR I have no idea, why sonar is complaining here (squid S3824)
      if (oldValue == null) {
        LOGGER.info(
            "Setting in consumer config: '{}'='{}' (forced from strategy {})",
            key,
            newValue,
            registration.getStrategy().getClass().getSimpleName());
      } else if (!newValue.equals(oldValue)) {
        LOGGER.warn(
            "Overwriting in consumer config: '{}'='{}' with new value '{}' (forced from strategy {})",
            key,
            oldValue,
            newValue,
            registration.getStrategy().getClass().getSimpleName());
      }
      newConfig.put(key, newValue);
    }
    consumerConfig.setConfig(newConfig);
  }

  /** Initial checks. Configuration mus be initialized */
  private void checkInit() {
    if (kafkaConfiguration == null) {
      throw new IllegalStateException("KafkaConfiguration not yet initialized!");
    }
  }

  private void setupManagedThreadManager(Environment environment) {
    environment
        .lifecycle()
        .manage(
            onShutdown(
                () -> {
                  shutdownConsumerThreads();
                  stopProducers();
                }));
  }

  private void stopProducers() {
    messageProducers.forEach(
        p -> {
          try {
            p.close();
          } catch (InterruptException ie) {
            LOGGER.error("Error closing producer", ie);
            Thread.currentThread().interrupt();
          }
        });
    topicProducerCounterSpec.unregister();
  }

  //
  // Builder
  //

  private void shutdownConsumerThreads() {
    threadedMessageListeners.forEach(l -> l.messageListener.stopConsumer());
    threadedMessageListeners.forEach(
        l -> {
          try {
            l.thread.join();
          } catch (InterruptedException e) {
            LOGGER.warn("Error while shutting down consumer threads", e);
            Thread.currentThread().interrupt();
          }
        });

    topicConsumerHistogram.unregister();
  }

  public interface InitialBuilder {
    /**
     * @param configurationProvider the method reference that provides the @{@link
     *     KafkaConfiguration} from the applications configurations class
     * @param <C> the type of the applications configuration class
     * @return the same builder instance
     */
    <C extends Configuration> FinalBuilder<C> withConfigurationProvider(
        @NotNull KafkaConfigurationProvider<C> configurationProvider);
  }

  public interface FinalBuilder<T extends Configuration> {

    /**
     * Disables the health check for Kafka. By disabling the health check the service can stay
     * healthy even if the connection to Kafka is disrupted, if Kafka is not essential to the
     * functionality. However, disabling it still registers an external health check to be able to
     * monitor the connection.
     *
     * @return the same builder instance
     */
    FinalBuilder<T> withoutHealthCheck();

    KafkaBundle<T> build();
  }

  public static class Builder<T extends Configuration> implements InitialBuilder, FinalBuilder<T> {

    private KafkaConfigurationProvider<T> configurationProvider;
    private boolean healthCheckDisabled = false;

    private Builder() {}

    private Builder(KafkaConfigurationProvider<T> configurationProvider) {
      this.configurationProvider = configurationProvider;
    }

    @Override
    public FinalBuilder<T> withoutHealthCheck() {
      healthCheckDisabled = true;
      return this;
    }

    @Override
    public KafkaBundle<T> build() {
      return new KafkaBundle<>(configurationProvider, healthCheckDisabled);
    }

    @Override
    public <C extends Configuration> FinalBuilder<C> withConfigurationProvider(
        KafkaConfigurationProvider<C> configurationProvider) {
      return new Builder<>(configurationProvider);
    }
  }

  private static class ThreadedMessageListener<K, V> {
    private final MessageListener<K, V> messageListener;
    private final Thread thread;

    private ThreadedMessageListener(MessageListener<K, V> messageListener, Thread thread) {
      this.messageListener = messageListener;
      this.thread = thread;
    }
  }
}
