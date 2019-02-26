# kafka-common
This module provides a [`KafkaBundle`](./src/main/java/org/sdase/commons/server/kafka/KafkaBundle.java) adds convenient 
functionality to create Kafka consumers, producers, and topics via configuration or Java DSL. It additionally provides a 
default [`MessageListener`](./src/main/java/org/sdase/commons/server/kafka/consumer/MessageListener.java) that 
implements a polling loop for kafka consumers. The user of this bundle must only implement the functional logic. 


## Usage
Add the following dependency:
```
compile 'org.sdase.commons:sda-commons-server-kafka:<current-version>'
```

**Dependencies**

| Group                        | Name                    | Version     | Description |
|------------------------------|-------------------------|-------------|-------------|
| org.apache.kafka             | kafka-clients           | 1.1.1       | Client API for Apache Kafka |
| com.github.ftrossbach | club-topicana-core | 0.1.0 | Helper for Topic description | 

**Bootstrap**

The bundle should be added as field to the application since it provides methods for the creation of `MessageProducer` and `MessageListener`.
The Builders for `MessageHandlerRegistration` and `ProducerRegistration` supports the user in the creation of these complex configurable objects. 
 
```
public class DemoApplication {
   private final KafkaBundle<AppConfiguration> kafkaBundle = KafkaBundle.builder().withConfigurationProvider(AppConfiguration::getKafka).build();
   private final MessageProducer<String, ProductBundle> producer;
      
   public void initialize(Bootstrap<AppConfiguration> bootstrap) {
      bootstrap.addBundle(kafkaBundle);
   }
         
   public void run(AppConfiguration configuration, Environment environment) throws Exception {
      // register with default consumer and listener config
      // The handler implements the actual logic for the message processing
      
      kafkaBundle
            .registerMessageHandler(MessageHandlerRegistration
                  .<String, String> builder()
                  .withDefaultListenerConfig()
                  .forTopic("topic") // replace topic with your topic name
                  .withDefaultConsumer()
                  .withHandler(record -> results.add(record.value())) // replace with your handler implementation
                  .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
                  .build());
                         
      // register with custom consumer and listener configuration (e.g. 2 instances, poll every minute)
      // method returns list of listeners, one for each instance
      List<MessageListener> listener = kafkaBundle
            .registerMessageHandler(MessageHandlerRegistration
                  .builder()
                  .withListenerConfig(ListenerConfig.builder().withPollInterval(60000).build(2))
                  .forTopic("topic") // replace topic with your topic name
                  .withConsumerConfig("consumer2") // use consumer config from config yaml
                  .withHandler(x -> result.add(x)) // replace with your handler implementation
                  .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
                  .build());
      
      // create own serializer       
      SDASerializer<ProductBundle> pbSerializer = new SDASerializer<>();
      
      // Create message producer with default KafkaProducer
      MessageProducer<String, ProductBundle> producer = kafkaBundle
            .registerProducerForTopic(ProducerRegistration
                  .<String, String> builder()
                  .forTopic("topic")         // simple topic definition (name only, partition and replication Factor 1) 
                  .withDefaultProducer()
                  .build());
                  
      // Create message with a detailed topic specification that checks the topic
      MessageProducer<String, String> producerConfigured = kafkaBundle
            .registerProducer(ProducerRegistration
                  .<String, String> builder()
                  .forTopic(
                        TopicConfigurationBuilder.builder("mytopic")
                        .withReplicationFactor(2)
                        .withPartitionCount(2)
                        .withConfig(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT)
                        .build()
                  )
                  .checkTopicConfiguration() // enforces that topic must be configured exactly as defined above
                  .createTopicIfMissing()    // creates the topic if no topic has been found
                  .withProducerConfig("producer1") // use producer config from config yaml
                  .build());
      
      // JSON Example
      List<MessageListener> jsonListener = kafkaBundle.registerMessageHandler(MessageHandlerRegistration
            .<String, SimpleEntity >builder()
            .withDefaultListenerConfig()
            .forTopic(topic)
                  .withDefaultConsumer()
                  .withValueDeserializer(new KafkaJsonDeserializer<>(new ObjectMapper(), SimpleEntity.class))
                  .withHandler(x -> resultsString.add(x.value().getName()))
                  .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
                  .build()
            );

      MessageProducer<String, SimpleEntity> jsonProducer = kafkaBundle.registerProducer(ProducerRegistration
            .<String, SimpleEntity>builder()
            .forTopic(topic)
            .withDefaultProducer()
            .withValueSerializer(new KafkaJsonSerializer<>(new ObjectMapper())).build());
   }
   
   // Optional: make producer available via CDI @Produces annotation
   @Produces
   public MessageProducer<String, ProductBundle> produceKafkaProductBundleProducer() {
      return producer;
   }
}
```

Note: CDI is not included within the bundle.

### Known Kafka Problems

There exists a known Kafka issue in the new consumer API [KAFAK-4740](https://issues.apache.org/jira/browse/KAFKA-4740) 
that throws potentially a `org.apache.kafka.commons.errors.SerializationException` when a record key/value can't be deserialized depending on 
deserializer implementation. This can result in an infinite loop because the poll is not committed and the next poll will throw this exception 
again. The wrapped deserialization approach offers a workaround where this exception is prevented and the processing can continue. 
But be aware that the `key` or `value` can be `null` in this case in both `MessageHandler.handle()` and `ErrorHandler.handleError()` methods. 
Another alternative is to implement your own Deserializer to have even more control and where you can potentially apply any fallback deserialization
strategy. 

```
public class DemoApplication {
   private final KafkaBundle<AppConfiguration> kafkaBundle = KafkaBundle.builder().withConfigurationProvider(AppConfiguration::getKafka).build();
   private final MessageProducer<String, ProductBundle> producer;
      
   public void initialize(Bootstrap<AppConfiguration> bootstrap) {
      bootstrap.addBundle(kafkaBundle);
   }
         
   public void run(AppConfiguration configuration, Environment environment) throws Exception {
      // register with default consumer and listener config
      // The handler implements the actual logic for the message processing
      
      // JSON Example with wrapped Deserializer to avoid DeseriliazationException, see Note below
      List<MessageListener> jsonListener = kafkaBundle.registerMessageHandler(MessageHandlerRegistration
            .<String, SimpleEntity >builder()
            .withDefaultListenerConfig()
            .forTopic(topic)
            .withDefaultConsumer()
            .withValueDeserializer(
                new WrappedNoSerializationErrorDeserializer<>(
                    new KafkaJsonDeserializer<>(new ObjectMapper(), SimpleEntity.class)))
            .withHandler(x -> {
               if (x.value() != null) {
                  resultsString.add(x.value().getName())
               }
            })  
            .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
            .build()
      );
   }
```

## Configuration
To configure KafkaBundle add the following `kafka` block to your Dropwizard config.yml. The following config snippet shows an example configuration with descriptive comments:
```YAML
kafka:
  # For testing without a kafka in integration tests, the bundle api can be disabled. No consumers and providers will be created 
  disabled: false
  # Timeout for request to the kafka broker used by admin clients
  # Admin client is used for checking and creating topics 
  adminClientrequestTimeoutMs: 2000
  # List of brokers to bootstrap consumer and provider connection
  brokers:
    - kafka-broker-1:9092 
    - kafka-broker-2:9092
    
  # Security information used for all consumers and providers to connect to kafka
  security :
    user: user
    password: password
    protocol: SASL_SSL
  # Map with consumer configurations. The key is used as name/id to address the configuration within the code. 
  consumers:
    # id/name of the consumer configuration
    consumer1:
      # name of the consumer group the consumer belongs to. If not defined, 'default' is the default
      group: newGroup
      config:
        # Deserializers can be set here within the configuration or within the builder DSL
        key.deserializer: org.apache.kafka.common.serialization.LongDeserializer
        value.deserializer: org.apache.kafka.common.serialization.LongDeserializer
    cons2:
      # if no attribute "group" is given, a default is used
      config:
         fetch.min.bytes: 3000
         auto.offset.reset: latest
  # Map with producer configurations. The key is used as name to address the configuration within the code.
  producers:
    # id/name of the producer config 
    producer1:
      # configuration key -> values as defined in the kafka documentation
      config:
        # Serializers can be set here within the configuration or within the builder DSL
        key.serializer: org.apache.kafka.common.serialization.LongSerializer
        value.serializer: org.apache.kafka.common.serialization.LongSerializer
        acks: all
        retries: 1000 
  # Map with topic configurations. The key is the name of the topic and is also used to address the configuration within the code
  # Topic descriptions can be used to validate the brokers topic configuration or to create new topics   
  topics:
    # id of the topic configuration
    topic1:
      # topic name
      name: topic1-name
      # partitions and replications factor are mandatory
      partitions: 2
      replicationFactor: 2
      # configuration key -> values as defined in the kafka documentation
      config:
        max.message.bytes: 1024
        retention.ms: 60480000
    topicName2:
      partitions: 1
      replicationFactor: 1
  # Map with listener configurations that can be used within MessageListener creation.
  listenerConfig:
    # id/name of the listener configuration
    async:
      # Defines the commit type used when using explicit commit instead of auto commit.
      # ASYNC: Async commit is used and CallbackMessageHandler can be used to implement logic when the commit returns
      # SYNC: (Default) Sync commit uses the synchronous commit call. Exception is thrown when commit fails. 
      commitType: ASYNC
      # Defines if explicit commit should be used. If false, explicit commit is used. Default: true
      useAutoCommitOnly: false
      # Number of listener instances that will be generated. If > 1, several KafkaConsumer are generated. Kafka assigns these consumers
      # to different partitions of the consumed topic. Number instances should be smaller or equal to the number of partitions.  
      instances: 1
      # If the topic check is configured within the DSL, the listener waits this amount of ms before checking topic existence again. 0 will disable existence check even when configured in DSL
      topicMissingRetryMs: 60000
```

### configuration value defaults (extending/changing the kafka defaults)
This are only the defaults that are explicitly set within the code of the bundle. All other properties depends on the actual broker configuration or the Kafka defaults are used. 

| Key | Value |
|-----|-------|
| disabled | false |
| adminClientRequestTimeoutMs | 5000 |

#### brokers
No defaults

#### security

| Key | Value |
|-----|-------|
| protocol | PLAINTEXT |

#### consumers
| Key | Value |
|-----|-------|
| group | default |
| config -> enable.auto.commit | true |
| config -> auto.commit.interval.ms | 1000 |
| config -> auto.offset.reset | earliest |
| config -> key.deserializer | org.apache.kafka.common.serialization.StringDeserializer |
| config -> value.deserializer | org.apache.kafka.common.serialization.StringDeserializer |

#### producers
| Key | Value |
|-----|-------|
| config -> acks | all |
| config -> retries | 0 |
| config -> linger.ms | 0 |
| config -> key.serializer | org.apache.kafka.common.serialization.StringSerializer |
| config -> value.serializer | org.apache.kafka.common.serialization.StringSerializer |

#### topics
| Key | Value |
|-----|-------|
| replicationFactor | 1 |
| partitions | 1 |

#### listenerConfig
| Key | Value |
|-----|-------|
| instances | 1 |
| commitType | SYNC |
| useAutoCommitOnly | true |
| topicMissingRetryMs | 0 |

## The MessageListener
The bundle provides a default [`MessageListener`](../sda-commons-server-kafka/src/main/java/org/sdase/commons/server/kafka/consumer/MessageListener.java) 
that reads messages from the broker and passes the records to a message handler that must be implemented by the user of the bundle.

The user can choose between auto-commit, sync, and async commits by configuration. But, the `MessageListener` does not implement
any extra logic in case of rebalancing. Therefore, the listener does not support an exactly once semantic. It might occur
that messages are redelivered after rebalance activities. 

## Migration information (from kafka-commons)

**Compatibility with older broker versions**

:exclamation: 
**Newer versions than kafka-commons v0.19.0 assumes at least version 1.0.0 of the broker, since some admin client commands are not supported in earlier broker versions**

If you use older versions of the broker, you MUST not use the options to check or create topics for MessageHandlers and MessageProducers.  

----

**Now each `MessageListener` uses exactly one thread to handle the incoming consumer records.** Commit is done after this thread returns from processing the implemented functional logic. 
In former versions of this bundle, there was a shared ThreadPool used by all `MessageListener`s. The business logic was handled in an own thread executed within the thread pool. 
Records has been committed directly after creating the threads and not after business logic execution. This hinder an ordered message processing as well as committing an offset after the 
business logic processing.    
This implementation must be considered when migrating older implementations since it might affect the performance. If you prefer the old behavior, you should create a thread pool within your
`MessageHandler` implementation.  


## Kafka Bundle with Managed Kafka
See below for example configuration. Of course, you can configure further properties, analogously.
```
kafka:
  brokers:
      ${KAFKA_BROKERS:-[]}
  security:
      user: ${SECRET_KAFKA_USERNAME}
      password: ${SECRET_KAFKA_PASSWORD}
      protocol: ${KAFKA_SECURITY_PROTOCOL}
  producers:
    producer1:
      config:
        key.serializer: org.apache.kafka.common.serialization.LongSerializer
        value.serializer: org.apache.kafka.common.serialization.LongSerializer
        acks: all
        ... 
```
_Note_: Do not use `;` in passwords, as this will crash your application.

In this case, the `KAFKA_BROKERS` variable should contain a Json array with a list of broker 

```json
[
  "kafka-broker:12345",
  "kafka-broker:54321"
]
```

## Health check

A health check with the name kafkaConnection is automatically registered to test the Kafka connection. The health check tries to list the topics available at the broker.

## Testing
[`sda-commons-server-kafka-testing`](../sda-commons-server-kafka-testing/README.md) provides support for integration testing with kafka with JUnit 4.

