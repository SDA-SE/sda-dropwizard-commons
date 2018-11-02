package com.sdase.commons.server.kafka;

import com.github.ftrossbach.club_topicana.core.ComparisonResult;
import com.github.ftrossbach.club_topicana.core.EvaluationException;
import com.github.ftrossbach.club_topicana.core.ExpectedTopicConfiguration;
import com.github.ftrossbach.club_topicana.core.MismatchedTopicConfigException;
import com.sdase.commons.server.kafka.builder.MessageHandlerRegistration;
import com.sdase.commons.server.kafka.builder.ProducerRegistration;
import com.sdase.commons.server.kafka.config.ConsumerConfig;
import com.sdase.commons.server.kafka.config.ListenerConfig;
import com.sdase.commons.server.kafka.config.ProducerConfig;
import com.sdase.commons.server.kafka.config.TopicConfig;
import com.sdase.commons.server.kafka.consumer.MessageListener;
import com.sdase.commons.server.kafka.exception.ConfigurationException;
import com.sdase.commons.server.kafka.producer.KafkaMessageProducer;
import com.sdase.commons.server.kafka.producer.MessageProducer;
import com.sdase.commons.server.kafka.topicana.TopicComparer;
import com.sdase.commons.server.kafka.topicana.TopicConfigurationBuilder;
import com.sdase.commons.server.kafka.exception.TopicCreationException;
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
import org.apache.kafka.common.errors.InterruptException;
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
import java.util.function.Function;
import java.util.stream.Collectors;

public class KafkaBundle<C extends Configuration> implements ConfiguredBundle<C> {

   private static final Logger LOGGER = LoggerFactory.getLogger(KafkaBundle.class);

   private final Function<C, KafkaConfiguration> configurationProvider;
   private KafkaConfiguration kafkaConfiguration;

   private List<MessageListener<?, ?>> messageListeners = new ArrayList<>();
   private List<Thread> consumerThreads = new ArrayList<>();
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
    * @param name
    *           the name of the topic
    * @return the configured topic configuration
    */
   public ExpectedTopicConfiguration getTopicConfiguration(String name) {
      if (topics.get(name) == null) {
         throw new ConfigurationException("Topic name seems not to be part of the read configuration. Please check the name and configuration.");
      }
      return topics.get(name);
   }

   /**
    * creates a MessageListener based on the data in the
    * {@link MessageHandlerRegistration}
    *
    * @param registration
    *           the configuration object
    * @param <K>
    *           key clazz type
    * @param <V>
    *           value clazz type
    * @return message listener
    */
   public <K, V> List<MessageListener<K, V>> registerMessageHandler(MessageHandlerRegistration<K, V> registration) {
      checkInit();
      if (registration.isCheckTopicConfiguration()) {
         ComparisonResult comparisonResult = checkTopics(registration.getTopics());
         if (!comparisonResult.ok()) {
            throw new MismatchedTopicConfigException(comparisonResult);
         }
      }

      List<MessageListener<K, V>> listener = new ArrayList<>(registration.getListenerConfig().getInstances());
      for (int i = 0; i < registration.getListenerConfig().getInstances(); i++) {
         ListenerConfig config = registration.getListenerConfig();
         if (config == null && registration.getListenerConfigName() != null){
            ListenerConfig listenerConfig = kafkaConfiguration.getListenerConfig().get(registration.getListenerConfigName());
            if (listenerConfig == null)  {
               throw new ConfigurationException(String.format("Listener config with name %s cannot be found within the current configuration.", registration.getListenerConfigName()));
            }
            config = kafkaConfiguration.getListenerConfig().get(registration.getListenerConfigName());
         }

         if (config == null) {
            throw new ConfigurationException("No valid listener config given within the MessageHandlerRegistration");
         }

         MessageListener<K, V> instance = new MessageListener<>(registration, createConsumer(registration), config);
         listener.add(instance);
         Thread t = new Thread(instance);
         t.start();
         consumerThreads.add(t);

      }
      messageListeners.addAll(listener);
      return listener;
   }

   /**
    * creates a @{@link MessageProducer} based on the data in the
    * {@link ProducerRegistration}
    *
    * @param registration
    *           the configuration object
    * @param <K>
    *           key clazz type
    * @param <V>
    *           value clazz type
    * @return message producer
    */
   @SuppressWarnings("unchecked")
   public <K, V> MessageProducer<K, V> registerProducer(ProducerRegistration<K, V> registration) {
      checkInit();

      if (registration.isCheckTopicConfiguration() || registration.isCreateTopicIfMissing()) {
         checkAndCreateTopic(Collections.singletonList(registration.getTopic()), registration.isCreateTopicIfMissing());
      }

      KafkaMessageProducer<K, V> messageProducer = new KafkaMessageProducer(registration.getTopicName(),
            createProducer(registration));

      messageProducers.add(messageProducer);
      return messageProducer;
   }

   /**
    * Checks or creates a collection of topics with respect to its configuration
    * 
    * @param topics
    *           Collection of topic configurations that should be checked
    * @param create
    *           defines if not existing topics should be created
    * 
    */

   public void checkAndCreateTopic(Collection<ExpectedTopicConfiguration> topics, boolean create) {
      // find out what topics are missing
      ComparisonResult comparisonResult = checkTopics(topics);
      if (!comparisonResult.getMissingTopics().isEmpty() && create) {
         createTopics(topics
               .stream()
               .filter(t -> comparisonResult.getMissingTopics().contains(t.getTopicName()))
               .collect(Collectors.toList()));
         // Recheck topics to remove created topics from comparision result
         ComparisonResult comparisonResult1 = checkTopics(topics);
         if (!comparisonResult1.ok()) {
            throw new MismatchedTopicConfigException(comparisonResult);
         }
         return;
      }
      if (!comparisonResult.ok()) {
         throw new MismatchedTopicConfigException(comparisonResult);
      }
   }

   /**
    * Checks if the defined topics are available on the broker for the provided
    * credentials
    * 
    * @param topics
    *           list of topics to test
    * 
    */
   public ComparisonResult checkTopics(Collection<ExpectedTopicConfiguration> topics) {
      try (final AdminClient adminClient = AdminClient.create(KafkaProperties.forAdminClient(kafkaConfiguration))) {
         TopicComparer topicComparer = new TopicComparer(adminClient);
         return topicComparer.compare(topics);
      }
   }

   public void createTopics(Collection<ExpectedTopicConfiguration> topics) {
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

            return new NewTopic(t.getTopicName(), replications, (short) partitions).configs(t.getProps());
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


   private <K, V> KafkaProperties getDefaultProducerProperties(ProducerRegistration<K, V> registration) {
      KafkaProperties kafkaProperties;
      if (registration.useAvro()) {
         kafkaProperties = KafkaProperties.forAvroProducer(kafkaConfiguration);
      } else {
         kafkaProperties = KafkaProperties.forProducer(kafkaConfiguration);
      }
      return kafkaProperties;
   }

   private <K, V> KafkaProperties getDefaultConsumerProperties(MessageHandlerRegistration<K, V> registration) {
      KafkaProperties kafkaProperties;
      if (registration.useAvro()) {
         kafkaProperties = KafkaProperties.forAvroConsumer(kafkaConfiguration);
      } else {
         kafkaProperties = KafkaProperties.forConsumer(kafkaConfiguration);
      }
      return kafkaProperties;
   }


   private ExpectedTopicConfiguration createTopicDescription(TopicConfig c) {
      return TopicConfigurationBuilder
            .builder(c.getName())
            .withPartitionCount(c.getPartitions())
            .withReplicationFactor(c.getReplicationFactor())
            .withConfig(c.getConfig())
            .build();
   }

   private <K, V> KafkaProducer<K,V> createProducer(ProducerRegistration<K, V> registration) {
      KafkaProperties producerProperties = getDefaultProducerProperties(registration);
      ProducerConfig producerConfig = registration.getProducerConfig();

      if (producerConfig == null && registration.getProducerConfigName() != null) {
         if (!kafkaConfiguration.getProducers().containsKey(registration.getProducerConfigName())) {
            throw new ConfigurationException(String.format("Producer config with name %s cannot be found within the current configuration.", registration.getProducerConfigName()));
         }
         producerConfig = kafkaConfiguration.getProducers().get(registration.getProducerConfigName());
      }

      if (producerConfig != null) {
         producerConfig.getConfig().forEach(producerProperties::put);
      }

      return new KafkaProducer<>(producerProperties, registration.getKeySerializer(), registration.getValueSerializer());
   }

   private <K, V> KafkaConsumer<K,V> createConsumer(MessageHandlerRegistration<K, V> registration) {
      KafkaProperties consumerProperties = getDefaultConsumerProperties(registration);
      ConsumerConfig consumerConfig = registration.getConsumerConfig();

      if (consumerConfig == null && registration.getConsumerConfigName() != null) {
         if (!kafkaConfiguration.getConsumers().containsKey(registration.getConsumerConfigName())) {
            throw new ConfigurationException(String.format("Consumer config with name %s cannot be found within the current configuration.", registration.getConsumerConfigName()));
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
      consumerThreads.forEach(t -> {
         try {
            t.join();
         } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            LOGGER.warn("KafkaBundle interrupted while trying to shutdown consumer threads!");
            Thread.currentThread().interrupt();
         }
      });
   }

   //
   // Builder
   //

   public static InitialBuilder builder() {
      return new Builder();
   }

   public interface InitialBuilder {
      /**
       * 
       * @param configurationProvider
       *           the method reference that provides
       *           the @{@link KafkaConfiguration} from the applications
       *           configurations class
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
