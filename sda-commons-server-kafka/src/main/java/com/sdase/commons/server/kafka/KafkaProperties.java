package com.sdase.commons.server.kafka;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class KafkaProperties extends Properties {

   /**
    * 
    */
   private static final long serialVersionUID = -4196815076149945888L;

   private KafkaProperties() {
      //
   }

   private static KafkaProperties baseProperties(KafkaConfiguration configuration) {
      KafkaProperties props = new KafkaProperties();

      if (configuration.getBrokers() != null) {
         props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
               String.join(",", configuration.getBrokers())
         );
      }
      if (configuration.getSecurity().getPassword() != null && configuration.getSecurity().getUser() != null
            && configuration.getSecurity().getProtocol() != null) {
         props.put("sasl.mechanism", "PLAIN");
         props.put("sasl.jaas.config",
               "org.apache.kafka.common.security.plain.PlainLoginModule required username='"
                     .concat(configuration.getSecurity().getUser())
                     .concat("' password='")
                     .concat(configuration.getSecurity().getPassword())
                     .concat("';"));
         props.put("security.protocol", configuration.getSecurity().getProtocol().name());
      }

      return props;
   }

   public static KafkaProperties forAdminClient(KafkaConfiguration configuration) {
      KafkaProperties props = baseProperties(configuration);
      props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, configuration.getAdminClientrequestTimeoutMs());
      return props;
   }

   public static KafkaProperties forConsumer(KafkaConfiguration configuration) {
      KafkaProperties props = baseProperties(configuration);

      props.put(ConsumerConfig.GROUP_ID_CONFIG, "default");
      props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
      props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, String.valueOf(1000));
      props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
      props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
      props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

      return props;
   }

   public static KafkaProperties forProducer(KafkaConfiguration configuration) {
      KafkaProperties props = baseProperties(configuration);
      props.put(ProducerConfig.ACKS_CONFIG, "all");
      props.put(ProducerConfig.RETRIES_CONFIG, "0");
      props.put(ProducerConfig.LINGER_MS_CONFIG, "0");
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

      return props;
   }

   static KafkaProperties forAvroConsumer(KafkaConfiguration configuration) {
      KafkaProperties props = forConsumer(configuration);

      props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "true");
      props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG,configuration.getSchemaRegistry().getUrl());
      props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());

      return props;
   }

   static KafkaProperties forAvroProducer(KafkaConfiguration configuration) {
      KafkaProperties props = forProducer(configuration);

      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
      props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, configuration.getSchemaRegistry().getUrl());

      return props;
   }


}
