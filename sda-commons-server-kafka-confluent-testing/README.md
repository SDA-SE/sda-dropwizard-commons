# SDA Commons Server Kafka Confluent Testing

This module adds support for testing with a Confluent Schema Registry. It provides a `ConfluentSchemaRegistryRule`
that will start a confluent schema registry connecting to a kafka broker. With this registry, you can use Confluent 
`KafkaAvroSerializer` and `KafkaAvroDeserilizer` (that are not included). Please use the same version for these serializers
than used for confluent schema registry provided with this project.

To use this module, you have to add the confluent artifact repository to your project build.grade file.
```
repositories {
   maven { url "http://packages.confluent.io/maven/" } 
}
```

## Usage
A running broker is mandatory for a Schema registry. The folloging snippet shows how to start a Kafka broker and the schema registry. 

```
   private static SharedKafkaTestResource kafkaTestResource = new SharedKafkaTestResource().withBrokers(2);

   private static WrappedSharedKafkaRule kafkaRule = new WrappedSharedKafkaRule(kafkaTestResource);

   @ClassRule
   public static final ConfluentSchemaRegistryRule SCHEMA_REGISTRY = ConfluentSchemaRegistryRule.builder()
         .withKafkaBrokerRule(kafkaRule)
         .withProtocol("PLAINTEXT")
         .withPort(9061)
         .build();      
```

