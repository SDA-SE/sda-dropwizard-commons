package org.sdase.commons.server.kafka;

import static org.sdase.commons.server.dropwizard.lifecycle.ManagedShutdownListener.onShutdown;

import io.dropwizard.core.Configuration;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.micrometer.core.instrument.Metrics;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.sdase.commons.server.dropwizard.metadata.MetadataContext;
import org.sdase.commons.server.kafka.builder.MessageListenerRegistration;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.config.ConsumerConfig;
import org.sdase.commons.server.kafka.config.ListenerConfig;
import org.sdase.commons.server.kafka.config.ProducerConfig;
import org.sdase.commons.server.kafka.config.TopicConfig;
import org.sdase.commons.server.kafka.consumer.MessageListener;
import org.sdase.commons.server.kafka.exception.ConfigurationException;
import org.sdase.commons.server.kafka.health.ExternalKafkaHealthCheck;
import org.sdase.commons.server.kafka.health.KafkaHealthCheck;
import org.sdase.commons.server.kafka.producer.KafkaMessageProducer;
import org.sdase.commons.server.kafka.producer.MessageProducer;
import org.sdase.commons.server.kafka.producer.MetadataContextAwareKafkaProducer;
import org.sdase.commons.server.kafka.producer.TraceTokenAwareKafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("WeakerAccess")
public class KafkaBundle<C extends Configuration> implements ConfiguredBundle<C> {

  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaBundle.class);

  public static final String HEALTHCHECK_NAME = "kafkaConnection";
  public static final String EXTERNAL_HEALTHCHECK_NAME = "kafkaConnectionExternal";

  private final Function<C, KafkaConfiguration> configurationProvider;
  private KafkaConfiguration kafkaConfiguration;
  private final boolean healthCheckDisabled;
  private final String healthCheckName;

  private final List<MessageListener<?, ?>> messageListeners = new ArrayList<>();
  private final List<ThreadedMessageListener<?, ?>> threadedMessageListeners = new ArrayList<>();
  private final Map<String, KafkaMessageProducer<?, ?>> messageProducers = new HashMap<>();

  private KafkaHealthCheck kafkaHealthCheck;

  private MicrometerProducerListener micrometerProducerListener;

  private MicrometerConsumerListener micrometerConsumerListener;

  private KafkaBundle(
      KafkaConfigurationProvider<C> configurationProvider,
      boolean healthCheckDisabled,
      String healthCheckName) {
    this.configurationProvider = configurationProvider;
    this.healthCheckDisabled = healthCheckDisabled;
    this.healthCheckName = healthCheckName;
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
    if (!kafkaConfiguration.isDisabled()) {
      if (healthCheckDisabled) {
        kafkaHealthCheck = new ExternalKafkaHealthCheck(kafkaConfiguration);
        environment
            .healthChecks()
            .register(
                this.healthCheckName != null ? this.healthCheckName : EXTERNAL_HEALTHCHECK_NAME,
                kafkaHealthCheck);
      } else {
        kafkaHealthCheck = new KafkaHealthCheck(kafkaConfiguration);
        environment
            .healthChecks()
            .register(
                this.healthCheckName != null ? this.healthCheckName : HEALTHCHECK_NAME,
                kafkaHealthCheck);
      }
    }

    micrometerProducerListener = new MicrometerProducerListener(Metrics.globalRegistry);
    micrometerConsumerListener = new MicrometerConsumerListener(Metrics.globalRegistry);
    setupManagedThreadManager(environment);
  }

  /**
   * Provides a {@link TopicConfig} that is generated from the values within the configuration yaml
   *
   * @param key the name of the topic configuration
   * @return the configured topic configuration
   * @throws ConfigurationException if no such topic exists in the configuration
   */
  public TopicConfig getTopicConfiguration(String key) throws ConfigurationException {
    if (kafkaConfiguration.getTopics().get(key) == null) {
      throw new ConfigurationException(
          String.format(
              "Topic with key '%s' seems not to be part of the read configuration. Please check the name and configuration.",
              key));
    }
    return kafkaConfiguration.getTopics().get(key);
  }

  /**
   * Creates a number of message listeners with the parameters given in the {@link
   * MessageListenerRegistration}.
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
      registration.getStrategy().init(MetadataContext.metadataFields());
      MessageListener<K, V> instance =
          new MessageListener<>(
              registration.getTopicsNames(),
              createConsumer(registration, i),
              listenerConfig,
              registration.getStrategy());

      listener.add(instance);
      Thread t = new Thread(instance);
      t.start();

      Optional<String> clientIdOptional =
          instance.getConsumer().metrics().entrySet().stream()
              .findFirst()
              .map(m -> m.getKey().tags().get("client-id"));

      threadedMessageListeners.add(
          new ThreadedMessageListener<>(instance, t, clientIdOptional.orElse("")));
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
      throws ConfigurationException {

    // if Kafka is disabled (for testing issues), we return a dummy producer
    // only.
    // This dummy works as long as the future is not evaluated
    if (kafkaConfiguration.isDisabled()) {
      return (k, v, h, c) -> null;
    }

    checkInit();

    Producer<K, V> producer = createProducer(registration);
    Entry<MetricName, ? extends Metric> entry =
        producer.metrics().entrySet().stream().findFirst().orElse(null);
    String clientId = entry != null ? entry.getKey().tags().get("client-id") : "";

    KafkaMessageProducer<K, V> messageProducer =
        new KafkaMessageProducer<>(registration.getTopic().getName(), producer);

    if (micrometerProducerListener == null) {
      LOGGER.warn("MicrometerProducerListener is not initialized! Metrics will not be recorded.");
    } else {
      micrometerProducerListener.producerAdded(clientId, producer);
    }

    messageProducers.put(clientId, messageProducer);
    return messageProducer;
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
    KafkaConsumer<K, V> consumer =
        new KafkaConsumer<>(consumerProperties, keyDeSerializer, valueDeSerializer);

    if (micrometerConsumerListener == null) {
      LOGGER.warn("MicrometerConsumerListener is not initialized! Metrics will not be recorded.");
    } else {
      micrometerConsumerListener.consumerAdded(
          consumerProperties.getProperty(
              org.apache.kafka.clients.consumer.ConsumerConfig.CLIENT_ID_CONFIG),
          consumer);
    }

    return consumer;
  }

  /**
   * Creates a new Kafka {@link Producer} with serializers and producer config
   *
   * <p>Note: after creating a Kafka {@code Producer} you must always {@link Producer#close()} it to
   * avoid resource leaks.
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
  public <K, V> Producer<K, V> createProducer(
      Serializer<K> keySerializer, Serializer<V> valueSerializer, ProducerConfig producerConfig) {
    KafkaProperties producerProperties = KafkaProperties.forProducer(kafkaConfiguration);

    if (producerConfig != null) {
      producerProperties.putAll(producerConfig.getConfig());
    }

    Producer<K, V> producer =
        new KafkaProducer<>(producerProperties, keySerializer, valueSerializer);
    Set<String> metadataFields = MetadataContext.metadataFields();
    if (!metadataFields.isEmpty()) {
      producer =
          new MetadataContextAwareKafkaProducer<>(producer, MetadataContext.metadataFields());
    }
    return new TraceTokenAwareKafkaProducer<>(producer);
  }

  /**
   * Creates a new Kafka {@link Producer} with serializers and producer config
   *
   * <p>Note: after creating a Kafka {@code Producer} you must always {@link Producer#close()} it to
   * avoid resource leaks.
   *
   * @param keySerializer deserializer for key objects. If null, value from config or default {@link
   *     org.apache.kafka.common.serialization.StringSerializer} will be used
   * @param valueSerializer deserializer for value objects. If null, value from config or efault
   *     {@link org.apache.kafka.common.serialization.StringSerializer} will be used
   * @param producerConfigName name of the producer config to be used
   * @param <K> Key object type
   * @param <V> Value object type
   * @return a new kafka producer
   */
  public <K, V> Producer<K, V> createProducer(
      Serializer<K> keySerializer, Serializer<V> valueSerializer, String producerConfigName) {

    ProducerConfig producerConfig = getProducerConfiguration(producerConfigName);
    if (producerConfig != null && producerConfig.getClientId() == null) {
      producerConfig.setClientId(producerConfigName);
    }

    return createProducer(keySerializer, valueSerializer, producerConfig);
  }

  private <K, V> Producer<K, V> createProducer(ProducerRegistration<K, V> registration) {

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
   * @throws ConfigurationException when the configuration does not exist
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
   * @throws ConfigurationException when the configuration does not exist
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
      String oldValue = newConfig.get(key);
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
    if (kafkaConfiguration == null
        || micrometerProducerListener == null
        || micrometerConsumerListener == null) {
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
                  shutdownKafkaHealthCheck();
                }));
  }

  private void stopProducers() {
    messageProducers.forEach(
        (k, v) -> {
          try {
            v.close();
          } catch (InterruptException ie) {
            LOGGER.error("Error closing producer", ie);
            Thread.currentThread().interrupt();
          } finally {
            micrometerProducerListener.producerRemoved(k);
          }
        });
  }

  //
  // Builder
  //

  private void shutdownConsumerThreads() {
    threadedMessageListeners.forEach(l -> l.messageListener.stopConsumer());
    threadedMessageListeners.forEach(l -> micrometerConsumerListener.consumerRemoved(l.clientId));
    threadedMessageListeners.forEach(
        l -> {
          try {
            l.thread.join();
          } catch (InterruptedException e) {
            LOGGER.warn("Error while shutting down consumer threads", e);
            Thread.currentThread().interrupt();
          }
        });
  }

  private void shutdownKafkaHealthCheck() {
    if (kafkaHealthCheck != null) {
      kafkaHealthCheck.shutdown();
    }
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

    /**
     * Defines a custom health check name for this kafka bundle. Defining a custom name is only
     * needed and should only be set, if multiple {@code KafkaBundle}s are used for multiple brokers
     * to create unique names.
     *
     * <p>An external health check, created when {@link #withoutHealthCheck()} is configured, uses
     * {@value #EXTERNAL_HEALTHCHECK_NAME} as default and a regular health check uses {@value
     * #HEALTHCHECK_NAME} as default.
     *
     * @param name the name of the health check
     * @return the same builder instance
     */
    FinalBuilder<T> withHealthCheckName(String name);

    KafkaBundle<T> build();
  }

  public static class Builder<T extends Configuration> implements InitialBuilder, FinalBuilder<T> {

    private KafkaConfigurationProvider<T> configurationProvider;
    private boolean healthCheckDisabled = false;
    private String healthCheckName = null;

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
    public FinalBuilder<T> withHealthCheckName(String name) {
      this.healthCheckName = name;
      return this;
    }

    @Override
    public KafkaBundle<T> build() {
      return new KafkaBundle<>(configurationProvider, healthCheckDisabled, healthCheckName);
    }

    @Override
    public <C extends Configuration> FinalBuilder<C> withConfigurationProvider(
        KafkaConfigurationProvider<C> configurationProvider) {
      return new Builder<>(configurationProvider);
    }
  }

  private record ThreadedMessageListener<K, V>(
      MessageListener<K, V> messageListener, Thread thread, String clientId) {}
}
