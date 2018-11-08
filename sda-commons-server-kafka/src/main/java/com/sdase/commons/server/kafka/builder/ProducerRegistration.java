package com.sdase.commons.server.kafka.builder;

import com.github.ftrossbach.club_topicana.core.ExpectedTopicConfiguration;
import com.sdase.commons.server.kafka.config.ProducerConfig;
import com.sdase.commons.server.kafka.exception.TopicMissingException;
import com.sdase.commons.server.kafka.topicana.TopicConfigurationBuilder;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.common.serialization.Serializer;

import javax.validation.constraints.NotNull;

public class ProducerRegistration<K, V> {


   private Serializer<K> keySerializer;
   private Serializer<V> valueSerializer;
   private ExpectedTopicConfiguration topic;
   private boolean useAvro = false;
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

   public boolean useAvro() {
      return useAvro;
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
       * @param topic the topic for that  messages will be produced
       * @return builder
       */
      ProducerBuilder<K, V> forTopic(String topic);

      /**
       * @param topic detailed definition of the topic for that messages will be produced. This details are used when topic
       *              existence should be checked or topic should be created if missing. If topic differs, a {@link TopicMissingException}
       *              is thrown.
       * @return builder
       */
      ProducerBuilder<K, V> forTopic(ExpectedTopicConfiguration topic);

   }


   public interface ProducerBuilder<K, V> {

      /**
       * defines that the topic configuration should be checked. If the name is given only, just topic existance is checked.
       * @return builder
       */
      ProducerBuilder<K, V> checkTopicConfiguration();

      /**
       * defines that the topic should be created if it does not exist
       * @return builder
       */
      ProducerBuilder<K, V> createTopicIfMissing();

      /**
       * defines that the default producer should be used
       * @return builder
       */
      FinalBuilder<K, V> withDefaultProducer();

      /**
       * @param config configuration used for KafkaProducer creation
       * @return builder
       */
      FinalBuilder<K, V> withProducerConfig(ProducerConfig config);

      /**
       * @param name name of the ProducerConfig that is defined in the coniguration yaml
       * @return builder
       */
      FinalBuilder<K, V> withProducerConfig(String name);

   }

   public interface FinalBuilder<K, V> {

      /**
       * @param keySerializer define a new key serializer
       * @return builder
       */
      FinalBuilder<K, V> withKeySerializer(Serializer<K> keySerializer);

      /**
       * @param valueSerializer define a new value serializer
       * @return builder
       */
      FinalBuilder<K, V> withValueSerializer(Serializer<V> valueSerializer);

      /**
       * defines that the avro value serializer should be used
       * @return builder
       */
      FinalBuilder<K, V> withAvroValueSerializer();

      ProducerRegistration<K, V> build();
   }

   /**
    * creates a new builder
    * @return builder
    */
   public static <K, V> TopicBuilder<K, V> builder() {
      return new Builder<>();
   }


   private static class Builder<K, V> implements TopicBuilder<K, V>, ProducerBuilder<K,V>,  FinalBuilder<K, V> {

      private Serializer<?> keySerializer = null;
      private Serializer<?> valueSerializer = null;
      private ExpectedTopicConfiguration topic;
      private boolean useAvro = false;
      private boolean checkTopicConfiguration = false;
      private boolean createTopicIfMissing = false;
      private ProducerConfig producerConfig;
      private String producerName = null;


      private Builder() {
      }

      @Override
      public ProducerBuilder<K, V> forTopic(@NotNull String topic) {
         this.topic = TopicConfigurationBuilder.builder(topic).build();
         return this;
      }

      @Override
      public ProducerBuilder<K, V> forTopic(@NotNull ExpectedTopicConfiguration topic) {
         this.topic = topic;
         return this;
      }

      @Override
      public FinalBuilder<K, V> withKeySerializer(@NotNull Serializer<K> keySerializer) {
         this.keySerializer = keySerializer;
         return this;
      }

      @Override
      public FinalBuilder<K, V> withValueSerializer(@NotNull Serializer<V> valueDeserializer) {
         this.valueSerializer = valueDeserializer;
         return this;
      }

      @Override
      public FinalBuilder<K, V> withAvroValueSerializer() {
         this.valueSerializer = new KafkaAvroSerializer();
         this.useAvro = true;
         return this;
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
      public FinalBuilder<K, V> withDefaultProducer() {
         this.producerConfig = null;
         this.producerName = null;
         return this;
      }

      @Override
      public FinalBuilder<K, V> withProducerConfig(ProducerConfig config) {
         this.producerConfig = config;
         return this;
      }

      @Override
      public FinalBuilder<K, V> withProducerConfig(String name) {
         this.producerName = name;
         return this;
      }

      @SuppressWarnings("unchecked")
      @Override
      public ProducerRegistration<K, V> build() {
         ProducerRegistration<K, V> build = new ProducerRegistration<>();
         build.keySerializer = (Serializer<K>) keySerializer;
         build.valueSerializer = (Serializer<V>) valueSerializer;
         build.topic = topic;
         build.useAvro = useAvro;
         build.checkTopicConfiguration = checkTopicConfiguration;
         build.createTopicIfMissing = createTopicIfMissing;
         build.producerConfig = producerConfig;
         build.producerName = producerName;

         return build;
      }
   }

}