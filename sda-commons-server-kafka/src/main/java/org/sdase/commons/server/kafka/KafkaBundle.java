package org.sdase.commons.server.kafka;

import com.github.ftrossbach.club_topicana.core.ComparisonResult;
import com.github.ftrossbach.club_topicana.core.EvaluationException;
import com.github.ftrossbach.club_topicana.core.ExpectedTopicConfiguration;
import com.github.ftrossbach.club_topicana.core.MismatchedTopicConfigException;
import org.sdase.commons.server.kafka.builder.MessageHandlerRegistration;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.config.ConsumerConfig;
import org.sdase.commons.server.kafka.config.ListenerConfig;
import org.sdase.commons.server.kafka.config.ProducerConfig;
import org.sdase.commons.server.kafka.config.TopicConfig;
import org.sdase.commons.server.kafka.consumer.MessageListener;
import org.sdase.commons.server.kafka.exception.ConfigurationException;
import org.sdase.commons.server.kafka.exception.TopicCreationException;
import org.sdase.commons.server.kafka.producer.KafkaMessageProducer;
import org.sdase.commons.server.kafka.producer.MessageProducer;
import org.sdase.commons.server.kafka.topicana.TopicComparer;
import org.sdase.commons.server.kafka.topicana.TopicConfigurationBuilder;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.header.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

public class KafkaBundle<C extends Configuration> implements ConfiguredBundle<C> {

   private static final Logger LOGGER = LoggerFactory.getLogger(KafkaBundle.class);

   private final Function<C, KafkaConfiguration> configurationProvider;
   private KafkaConfiguration kafkaConfiguration;

   private List<MessageListener<?, ?>> messageListeners = new ArrayList<>();
   private List<KafkaMessageProducer<?, ?>> messageProducers = new ArrayList<>();

   private Map<String, ExpectedTopicConfiguration> topics = new HashMap<>();

   private KafkaBundle(KafkaConfigurationProvider<C> configurationProvider) {
      this.configurationProvider = configurationProvider;
   }

   @Override
   public void initialize(Bootstrap<?> bootstrap) {
      //
   }

   @Override
   public void run(C configuration, Environment environment) {
      kafkaConfiguration = configurationProvider.apply(configuration);
      kafkaConfiguration.getTopics().forEach((k, v) -> topics.put(k, createTopicDescription(v)));
      setupManagedThreadManager(environment);
   }


   /**
    * Provides a {@link ExpectedTopicConfiguration} that is generated from the
    * values within the configuration yaml
    *
    * @param name the name of the topic
    * @return the configured topic configuration
    * @throws ConfigurationException if no such topic exists in the configuration
    */
   @SuppressWarnings("WeakerAccess")
   public ExpectedTopicConfiguration getTopicConfiguration(String name) throws ConfigurationException { // NOSONAR
      if (topics.get(name) == null) {
         throw new ConfigurationException(
             String.format(
                 "Topic with name '%s' seems not to be part of the read configuration. Please check the name and configuration.",
                 name));
      }
      return topics.get(name);
   }

   /**
    * creates a MessageListener based on the data in the
    * {@link MessageHandlerRegistration}
    *
    * @param registration the configuration object
    * @param <K>          key clazz type
    * @param <V>          value clazz type
    * @return message listener
    * @throws ConfigurationException if the {@code registration} has no
    *       {@link MessageHandlerRegistration#getListenerConfig()} and there is no configuration available with the same
    *       name as defined in {@link MessageHandlerRegistration#getListenerConfigName()}
    */
   @SuppressWarnings("WeakerAccess")
   public <K, V> List<MessageListener<K, V>> registerMessageHandler(MessageHandlerRegistration<K, V> registration)
         throws ConfigurationException { // NOSONAR
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
         listenerConfig = kafkaConfiguration.getListenerConfig().get(registration.getListenerConfigName());
         if (listenerConfig == null) {
            throw new ConfigurationException(String.format(
                "Listener config with name '%s' cannot be found within the current configuration.",
                registration.getListenerConfigName()));
         }
      }

      if (listenerConfig == null) {
         throw new ConfigurationException("No valid listener config given within the MessageHandlerRegistration");
      }

      List<MessageListener<K, V>> listener = new ArrayList<>(listenerConfig.getInstances());
      for (int i = 0; i < listenerConfig.getInstances(); i++) {
         MessageListener<K, V> instance = new MessageListener<>(registration, createConsumer(registration), listenerConfig);
         listener.add(instance);
         Thread t = new Thread(instance);
         t.start();
      }
      messageListeners.addAll(listener);
      return listener;
   }

   /**
    * creates a @{@link MessageProducer} based on the data in the
    * {@link ProducerRegistration}
    *
    * if the kafka bundle is disabled, null is returned
    *
    * @param registration the configuration object
    * @param <K>          key clazz type
    * @param <V>          value clazz type
    * @return message producer
    * @throws ConfigurationException if the {@code registration} has no
    *       {@link ProducerRegistration#getProducerConfig()} and there is no configuration available with the same name
    *       as defined in {@link ProducerRegistration#getProducerConfigName()}
    */
   @SuppressWarnings({"unchecked", "WeakerAccess"})
   public <K, V> MessageProducer<K, V> registerProducer(ProducerRegistration<K, V> registration)
         throws ConfigurationException { // NOSONAR

      // if kafka is disabled (for testing issues), we return a dummy producer only.
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
         ComparisonResult comparisonResult = checkTopics(Collections.singletonList(registration.getTopic()));
         if (!comparisonResult.ok()) {
            throw new MismatchedTopicConfigException(comparisonResult);
         }
      }

      KafkaMessageProducer<K, V> messageProducer = new KafkaMessageProducer(registration.getTopicName(),
            createProducer(registration));

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
         createTopics(topics
               .stream()
               .filter(t -> comparisonResult.getMissingTopics().contains(t.getTopicName()))
               .collect(Collectors.toList()));
      }
   }

   /**
    * Checks if the defined topics are available on the broker for the provided
    * credentials
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
      try (final AdminClient adminClient = AdminClient.create(KafkaProperties.forAdminClient(kafkaConfiguration))) {
         List<NewTopic> topicList = topics.stream().map(t -> {
            int partitions = 1;
            int replications = 1;
            if (!t.getPartitions().isSpecified()) {
               LOGGER.warn("Partitions for topic '{}' is not specified. Using default value 1", t.getTopicName());
            } else {
               partitions = t.getPartitions().count();
            }

            if (!t.getReplicationFactor().isSpecified()) {
               LOGGER
                     .warn("Replication factor for topic '{}' is not specified. Using default value 1",
                           t.getTopicName());
            } else {
               replications = t.getReplicationFactor().count();
            }

            return new NewTopic(t.getTopicName(),  partitions, (short) replications).configs(t.getProps());
         }).collect(Collectors.toList());
         CreateTopicsResult result = adminClient.createTopics(topicList);
         result.all().get();
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         throw new EvaluationException("Exception during adminClient.createTopics", e);
      } catch (ExecutionException e) {
         throw new TopicCreationException("TopicConfig creation failed", e);
      }
   }


   private ExpectedTopicConfiguration createTopicDescription(TopicConfig c) {
      return TopicConfigurationBuilder
            .builder(c.getName())
            .withPartitionCount(c.getPartitions())
            .withReplicationFactor(c.getReplicationFactor())
            .withConfig(c.getConfig())
            .build();
   }

   private <K, V> KafkaProducer<K, V> createProducer(ProducerRegistration<K, V> registration)
         throws ConfigurationException { // NOSONAR
      KafkaProperties producerProperties = KafkaProperties.forProducer(kafkaConfiguration);
      ProducerConfig producerConfig = registration.getProducerConfig();

      if (producerConfig == null && registration.getProducerConfigName() != null) {
         if (!kafkaConfiguration.getProducers().containsKey(registration.getProducerConfigName())) {
            throw new ConfigurationException(String.format(
                "Producer config with name '%s' cannot be found within the current configuration.",
                registration.getProducerConfigName()));
         }
         producerConfig = kafkaConfiguration.getProducers().get(registration.getProducerConfigName());
      }

      if (producerConfig != null) {
         producerConfig.getConfig().forEach(producerProperties::put);
      }

      return new KafkaProducer<>(producerProperties, registration.getKeySerializer(), registration.getValueSerializer());
   }

   private <K, V> KafkaConsumer<K, V> createConsumer(MessageHandlerRegistration<K, V> registration)
         throws ConfigurationException { // NOSONAR
      KafkaProperties consumerProperties = KafkaProperties.forConsumer(kafkaConfiguration);
      ConsumerConfig consumerConfig = registration.getConsumerConfig();

      if (consumerConfig == null && registration.getConsumerConfigName() != null) {
         if (!kafkaConfiguration.getConsumers().containsKey(registration.getConsumerConfigName())) {
            throw new ConfigurationException(String.format(
                "Consumer config with name '%s' cannot be found within the current configuration.",
                registration.getConsumerConfigName()));
         }
         consumerConfig = kafkaConfiguration.getConsumers().get(registration.getConsumerConfigName());

      }

      if (consumerConfig != null) {
         consumerProperties.putAll(consumerConfig.getConfig());
      }

      return new KafkaConsumer<>(consumerProperties, registration.getKeyDeserializer(), registration.getValueDeserializer());
   }


   /**
    * Initial checks. Configuration mus be initialized
    */
   private void checkInit() {
      if (kafkaConfiguration == null) {
         throw new IllegalStateException("KafkaConfiguration not yet initialized!");
      }
   }

   private void setupManagedThreadManager(Environment environment) {
      Managed threadManager = new Managed() {

         @Override
         public void stop() {
            shutdownConsumerThreads();
            stopProducers();
         }

         @Override
         public void start() {
            //
         }
      };

      environment.lifecycle().manage(threadManager);
   }

   private void stopProducers() {
      messageProducers.forEach(p -> {
         try {
            p.close();
         } catch (InterruptException ie) {
            LOGGER.error("Error closing producer", ie);
            Thread.currentThread().interrupt();
         }
      });
   }

   private void shutdownConsumerThreads() {
      messageListeners.forEach(MessageListener::stopConsumer);
   }

   //
   // Builder
   //

   public static InitialBuilder builder() {
      return new Builder();
   }

   public interface InitialBuilder {
      /**
       * @param configurationProvider the method reference that provides
       *                              the @{@link KafkaConfiguration} from the applications
       *                              configurations class
       */
      <C extends Configuration> FinalBuilder<C> withConfigurationProvider(
            @NotNull KafkaConfigurationProvider<C> configurationProvider);
   }

   public interface FinalBuilder<T extends Configuration> {
      KafkaBundle<T> build();
   }

   public static class Builder<T extends Configuration> implements InitialBuilder, FinalBuilder<T> {

      private KafkaConfigurationProvider<T> configurationProvider;

      private Builder() {
      }

      private Builder(KafkaConfigurationProvider<T> configurationProvider) {
         this.configurationProvider = configurationProvider;
      }

      @Override
      public KafkaBundle<T> build() {
         return new KafkaBundle<>(configurationProvider);
      }

      @Override
      public <C extends Configuration> FinalBuilder<C> withConfigurationProvider(
            KafkaConfigurationProvider<C> configurationProvider) {
         return new Builder<>(configurationProvider);
      }
   }

}
