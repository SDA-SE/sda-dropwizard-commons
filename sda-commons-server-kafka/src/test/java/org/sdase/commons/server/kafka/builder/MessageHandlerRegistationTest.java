package org.sdase.commons.server.kafka.builder;

import static org.assertj.core.api.Assertions.assertThat;

import org.sdase.commons.server.kafka.config.ConsumerConfig;
import org.sdase.commons.server.kafka.config.ListenerConfig;
import org.sdase.commons.server.kafka.consumer.strategies.legacy.CallbackMessageHandler;
import org.sdase.commons.server.kafka.consumer.ErrorHandler;
import org.sdase.commons.server.kafka.consumer.MessageHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MessageHandlerRegistationTest {

   @Test
   public void handlerIsRegisteredCorrectly() {

      MessageHandler<String, String> messageHandler = record -> {};
      ErrorHandler<String, String> errorHandler = (record, e, consumer) -> true;

      MessageHandlerRegistration<String, String> registration = MessageHandlerRegistration
            .<String, String> builder()
            .withListenerConfig(ListenerConfig.getDefault())
            .forTopic("TOPIC1")
            .withDefaultConsumer()
            .withKeyDeserializer(new StringDeserializer())
            .withValueDeserializer(new StringDeserializer())
            .withHandler(messageHandler)
            .withErrorHandler(errorHandler)
            .build();

      assertThat(registration.getTopicsNames()).contains("TOPIC1");
      assertThat(registration.getHandler()).isEqualTo(messageHandler);
      assertThat(registration.getKeyDeserializer()).isInstanceOf(StringDeserializer.class);
      assertThat(registration.getValueDeserializer()).isInstanceOf(StringDeserializer.class);

      CallbackMessageHandler<String, String> cbHandler = new CallbackMessageHandler<String, String>() {

         @Override
         public void handle(ConsumerRecord<String, String> record) {
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
            .withErrorHandler(errorHandler)
            .build();

      assertThat(registration1.getTopicsNames()).containsExactlyInAnyOrder("Topic1", "Topic2");
      assertThat(registration1.getHandler()).isEqualTo(cbHandler);
      assertThat(registration1.getKeyDeserializer()).isInstanceOf(StringDeserializer.class);
      assertThat(registration1.getValueDeserializer()).isInstanceOf(StringDeserializer.class);
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
            .withErrorHandler(((record, e, consumer) -> true))
            .build();

      assertThat(registration).isNotNull();
      assertThat(registration.getKeyDeserializer()).isInstanceOf(IntegerDeserializer.class);
      assertThat(registration.getValueDeserializer()).isInstanceOf(LongDeserializer.class);
      assertThat(registration.getListenerConfig().getPollInterval()).isEqualTo(99L);
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
            .withErrorHandler(((record, e, consumer1) -> true))
            .build();

      assertThat(registration).isNotNull();
      assertThat(registration.getConsumerConfig()).isEqualTo(consumer);
   }


}
