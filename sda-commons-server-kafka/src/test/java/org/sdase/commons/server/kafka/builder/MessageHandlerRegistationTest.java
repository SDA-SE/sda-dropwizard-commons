package org.sdase.commons.server.kafka.builder;

import org.sdase.commons.server.kafka.config.ConsumerConfig;
import org.sdase.commons.server.kafka.config.ListenerConfig;
import org.sdase.commons.server.kafka.consumer.CallbackMessageHandler;
import org.sdase.commons.server.kafka.consumer.KafkaMessageHandlingException;
import org.sdase.commons.server.kafka.consumer.MessageHandler;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class MessageHandlerRegistationTest {

   @Test
   public void handlerIsRegisteredCorrectly() {

      MessageHandler<String, String> messageHandler = record -> {};

      MessageHandlerRegistration<String, String> registration = MessageHandlerRegistration
            .<String, String> builder()
            .withListenerConfig(ListenerConfig.getDefault())
            .forTopic("TOPIC1")
            .withDefaultConsumer()
            .withKeyDeserializer(new StringDeserializer())
            .withValueDeserializer(new StringDeserializer())
            .withHandler(messageHandler)
            .build();

      assertThat(registration.getTopicsNames(), contains("TOPIC1"));
      assertThat(registration.getHandler(), equalTo(messageHandler));
      assertThat(registration.getKeyDeserializer(), instanceOf(StringDeserializer.class));
      assertThat(registration.getValueDeserializer(), instanceOf(StringDeserializer.class));

      CallbackMessageHandler<String, String> cbHandler = new CallbackMessageHandler<String, String>() {

         @Override
         public void handle(ConsumerRecord<String, String> record) throws KafkaMessageHandlingException {
         }

         @Override
         public void handleCommitCallback(Map<TopicPartition, OffsetAndMetadata> offsets, Exception exception) {
         }
      };

      MessageHandlerRegistration<String, String> registration1 = MessageHandlerRegistration
            .<String, String> builder()
            .withListenerConfig(ListenerConfig.getDefault())
            .forTopics(Arrays.asList("Topic1", "Topic2"))
            .withDefaultConsumer()
            .withKeyDeserializer(new StringDeserializer())
            .withValueDeserializer(new StringDeserializer())
            .withHandler(cbHandler)
            .build();

      assertThat(registration1.getTopicsNames(), containsInAnyOrder("Topic1", "Topic2"));
      assertThat(registration1.getHandler(), equalTo(cbHandler));
      assertThat(registration1.getKeyDeserializer(), instanceOf(StringDeserializer.class));
      assertThat(registration1.getValueDeserializer(), instanceOf(StringDeserializer.class));
   }

   @Test
   public void serializerAndPollIntervalAreSetCorrectly() {
      MessageHandler<Integer, Long> messageHandler = record -> {
      };

      MessageHandlerRegistration<Integer, Long> registration = MessageHandlerRegistration
            .<Integer, Long> builder()
            .withListenerConfig(ListenerConfig.builder().withPollInterval(99).build(1))
            .forTopic("Bla")
            .withDefaultConsumer()
            .withKeyDeserializer(new IntegerDeserializer())
            .withValueDeserializer(new LongDeserializer())
            .withHandler(messageHandler)
            .build();

      assertThat(registration, is(notNullValue()));
      assertThat(registration.getKeyDeserializer(), instanceOf(IntegerDeserializer.class));
      assertThat(registration.getValueDeserializer(), instanceOf(LongDeserializer.class));
      MatcherAssert.assertThat(registration.getListenerConfig().getPollInterval(), is(99L));
   }


   @Test
   public void customConsumerCanBeUsed() {
      MessageHandler<String, String> messageHandler = record -> {
      };

      Map<String, String> test = new HashMap<>();
      test.put("key.deserializer", StringDeserializer.class.getName());
      test.put("value.deserializer", StringDeserializer.class.getName());
      test.put("bootstrap.servers", "localhost:9092");

      ConsumerConfig consumer = new ConsumerConfig();
      consumer.getConfig().putAll(test);

      MessageHandlerRegistration<String, String> registration = MessageHandlerRegistration
            .<String, String> builder()
            .withListenerConfig(ListenerConfig.getDefault())
            .forTopic("Bla")
            .withConsumerConfig(consumer)
            .withHandler(messageHandler)
            .build();

      assertThat(registration, is(notNullValue()));
      assertThat(registration.getConsumerConfig(), is(consumer));
   }

   @Test
   public void avroDeSerializerUsed() {
      MessageHandler<Integer, KafkaAvroDeserializer> messageHandler = record -> {
      };

      MessageHandlerRegistration<Integer, KafkaAvroDeserializer> registration = MessageHandlerRegistration
            .<Integer, KafkaAvroDeserializer> builder()
            .withListenerConfig(ListenerConfig.getDefault())
            .forTopic("Bla")
            .withDefaultConsumer()
            .withKeyDeserializer(new IntegerDeserializer())
            .withAvroValueDeserializer()
            .withHandler(messageHandler)
            .build();

      assertThat(registration, is(notNullValue()));
      assertThat(registration.getKeyDeserializer(), instanceOf(IntegerDeserializer.class));
      assertThat(registration.getValueDeserializer(), instanceOf(KafkaAvroDeserializer.class));
         }

   @Test
   public void avroGivenDeserializersUsedEvenIfConsumerConfigIsDifferent() {
      MessageHandler<Integer, KafkaAvroDeserializer> messageHandler = record -> {
      };

      ConsumerConfig consumerConfig = new ConsumerConfig();
      consumerConfig.getConfig().put("value.deserializer", StringDeserializer.class.getName());
      consumerConfig.getConfig().put("key.deserializer", StringDeserializer.class.getName());

      MessageHandlerRegistration<Integer, KafkaAvroDeserializer> registration = MessageHandlerRegistration
            .<Integer, KafkaAvroDeserializer> builder()
            .withListenerConfig(ListenerConfig.getDefault())
            .forTopic("Bla")
            .withConsumerConfig(consumerConfig)
            .withKeyDeserializer(new IntegerDeserializer())
            .withAvroValueDeserializer()
            .withHandler(messageHandler)
            .build();

      assertThat(registration, is(notNullValue()));
      assertThat(registration.getKeyDeserializer(), instanceOf(IntegerDeserializer.class));
      assertThat(registration.getValueDeserializer(), instanceOf(KafkaAvroDeserializer.class));
   }


}
