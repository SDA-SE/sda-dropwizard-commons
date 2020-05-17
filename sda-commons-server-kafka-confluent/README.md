# SDA Commons Server Kafka Confluent

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-kafka-confluent/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-kafka-confluent)

The module `sda-commons-server-kafka-confluent` is the base module to add Avro specific support to Kafka.

[Apache Avro™](https://avro.apache.org/) is a serialization system and is often used in larger systems in the context of Kafka. 
Confluent Inc. provides it's own Kafka framework with smart integration of Avro already and the provided KafkaAvroSerializer and
KafkaAvroDerializer can be used directly in context sda-commons-server-kafka.

It includes the dependencies to [sda-commons-server-kafka](../sda-commons-server-kafka/README.md) module.

For Avro, the following libraries are included

| Group            | Artifact           | Version |
|------------------|--------------------|---------|
| io.confluent | kafka-avro-serializer | 4.1.2 |


## Usage of WrappedAvroDeserializer
The WrappedAvroDeserializer is a small builder that can be used to create a wrapped KafkaAvroDeserializer. The KafkaAvroDeserializer throws a Serialization exception
in case if any problems which can trigger a known problem in the latest Kafka consumer API. The wrapped deserializer can be used to avoid this exception and returns a
null value instead (key/value). The client MessageHandler implementation has to take care of possible nul values and can react in a proper way.  

```
   ConsumerConfig config = new ConsumerConfig();
   config.getConfig().put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "true");
   config.getConfig().put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY.getConnectionString());

   Deserializer<FullName> valueDeserializer = WrappedAvroDeserializer.<FullName>builder()
       .withClassType(FullName.class)
       .withConfigProperties(config.getConfig())
       .build(false);

   List<MessageListener<String, FullName>> stringStringMessageListener = bundle
       .registerMessageHandler(MessageHandlerRegistration
           .<String, FullName> builder()
           .withDefaultListenerConfig()
           .forTopic(topicName)
           .withConsumerConfig(config)
           .withValueDeserializer(valueDeserializer)
           .withHandler(
               record -> {
                 if (record.value() != null) {
                   results.add(record.value().getFirst());
                 }
               }
           )
           .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
           .build());
```

