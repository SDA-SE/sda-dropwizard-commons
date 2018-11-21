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
   
   @ClassRule
   public static final ConfluentSchemaRegistryRule SCHEMA_REGISTRY = ConfluentSchemaRegistryRule.builder()
          .withKafkaBrokerRule(new WrappedSharedKafkaRule(kafkaTestResource))
          .build();

```

## Example for Confluent AVRO Serializers Test
An example for using Confluent AVRO Serializers/Deserializers within the kafka bundle, can be found within the
bundle test class  [`KafkaAvroIT`](../sda-commons-server-kafka/src/integTest/java/org/sdase/commons/server/kafka/KafkaAvroIT.java)

You need to configure the serializers via the config objects since the Confluent classes are not designed using generics.

The following listing shows how to include the serializers.
```
compile group: 'io.confluent', name: 'kafka-avro-serializer', version: '4.1.2',  {
  exclude group: 'org.slf4j', module: 'slf4j-log4j12'
  exclude group: 'org.slf4j', module: 'slf4j-api'
  exclude group: 'org.apache.kafka', module: 'kafka-clients'
  exclude group: 'org.apache.zookeeper', module: 'zookeeper'
  exclude group: 'org.xerial.snappy', module: 'snappy-java'
}
```

If you use the AVRO plugin to generate Java classes, you must include the following snippet.
```
buildscript {
    repositories {
        jcenter()
        maven {
            url 'https://nexus.dev.sda-se.com/repository/sda-se-releases/'
            credentials {
                username sdaNexusUser
                password sdaNexusPassword
            }
        }
    }
    dependencies {
        classpath 'com.commercehub.gradle.plugin:gradle-avro-plugin:0.10.0'
    }
}

apply plugin: 'com.commercehub.gradle.plugin.avro'

// required for plugin 
compile ('org.apache.avro:avro:1.8.1') {
  exclude group: 'org.slf4j', module: 'slf4j-log4j12'
  exclude group: 'org.slf4j', module: 'slf4j-api'
  exclude group: 'org.xerial.snappy', module: 'snappy-java'
}
```