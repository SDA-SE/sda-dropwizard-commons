# SDA Commons Server Kafka
This module provides a [`KafkaBundle`](./src/main/java/org/sdase/commons/server/kafka/KafkaBundle.java) adds convenient 
functionality to create Kafka consumers, producers, and topics via configuration or Java DSL. 

It additionally provides a default [`MessageListener`](./src/main/java/org/sdase/commons/server/kafka/consumer/MessageListener.java) that 
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

The bundle got enhanced to allow more control and flexibility how kafka messages are consumed and which commit strategy is used. How to use
the old and now deprecated `KafkaBundle::registerMessageHandler` approach is documented [here](docs/deprecated.md).
     
The bundle should be added as field to the application since it provides methods for the creation of `MessageProducer` and `MessageListener`.
The Builders for `MessageListenerRegistration` and `ProducerRegistration` supports the user in the creation of these complex configurable objects. 
 
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
      
      // replace with your handler implementation
      MessageHandler<String, String> handler = record -> results.add(record.value();
      ErrorHandler<Sting, String> errorHandler = new IgnoreAndProceedErrorHandler<>()
      
      kafkaBundle.createMessageListener(MessageListenerRegistration.<String, String> builder()
                .withDefaultListenerConfig()
                .forTopic(topic) // replace topic with your topic name
                .withDefaultConsumer()
                .withValueDeserializer(new StringDeserializer())
                .withListenerStrategy(new AutocommitMLS<String, String>(handler, errorHandler))
                .build());
      
      // register with custom consumer and listener configuration (e.g. 2 instances, poll every minute)
      // method returns list of listeners, one for each instance
      List<MessageListener> listener = kafkaBundle
            .createMessageListener(MessageListenerRegistration.<String, String> builder()
                  .withListenerConfig(ListenerConfig.builder().withPollInterval(60000).build(2))
                  .forTopic("topic") // replace topic with your topic name
                  .withConsumerConfig("consumer2") // use consumer config from config yaml
                  .withListenerStrategy(new AutocommitMLS<String, String>(handler, errorHandler))
                  .build());      
      
      // Create message producer with default KafkaProducer
      MessageProducer<String, ProductBundle> producer = kafkaBundle
            .registerProducerForTopic(ProducerRegistration
                  .<String, String> builder()
                  .forTopic("topic")         // simple topic definition (name only, partition and replication Factor 1) 
                  .withDefaultProducer()
                  .build());
                  
      // Create message with a detailed topic specification that checks the topic
      MessageProducer<String, String> producerConfigured = kafkaBundle
            .registerProducer(ProducerRegistration.<String, String> builder()
                  .forTopic(TopicConfigurationBuilder.builder("mytopic")
                  .withReplicationFactor(2)
                  .withPartitionCount(2)
                  .withConfig(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT).build())
                  .checkTopicConfiguration() // enforces that topic must be configured exactly as defined above
                  .createTopicIfMissing()    // creates the topic if no topic has been found
                  .withProducerConfig("producer1") // use producer config from config yaml
                  .build());
      
      // JSON Example
      MessageHandler<String, SimpleEntity> jsonHandler = record -> jsonResults.add(record.value();
      ErrorHandler<Sting, SimpleEntity> errorHandler = new IgnoreAndProceedErrorHandler<>()
            
      List<MessageListener> jsonListener = kafkaBundle
            .createMessageListener(MessageListenerRegistration.<String, SimpleEntity >builder()
                  .withDefaultListenerConfig()
                  .forTopic(topic)
                  .withDefaultConsumer()
                  .withValueDeserializer(new KafkaJsonDeserializer<>(new ObjectMapper(), SimpleEntity.class))
                  .withListenerStrategy(new AutocommitMLS<String, SimpleEntity>(jsonHandler, errorHandler))
                  .build()
            );

      MessageProducer<String, SimpleEntity> jsonProducer = kafkaBundle.registerProducer(ProducerRegistration
            .<String, SimpleEntity>builder()
            .forTopic(topic)
            .withDefaultProducer()
            .withValueSerializer(new KafkaJsonSerializer<>(new ObjectMapper())).build());
            
      // plain consumer where the user has full control and take over responsibility to close te consumer
      // by name of a valid consumer configuration from config yaml
      KafkaConsumer<String, String> consumer = kafkaBundle.createConsumer(
            new StringDeserializer(), new StringDeserializer(), "consumer1");
      
      // by given consumer configuration      
      KafkaConsumer<String, String> consumer = kafkaBundle.createConsumer(
            new StringDeserializer(), new StringDeserializer(), ConsumerConfig.<String, String>builder()
                .withGroup("test-consumer")
                .addConfig("max.poll.records", "100")
                .addConfig("enable.auto.commit", "false").build());
                
      // There are similar methods for producer creation                   
   }
   
   // Optional: make producer available via CDI @Produces annotation
   // Note: CDI is not included within the bundle.
   @Produces
   public MessageProducer<String, ProductBundle> produceKafkaProductBundleProducer() {
      return producer;
   }
}
```

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
      ...
      
      // JSON Example with wrapped Deserializer to avoid DeseriliazationException, see Note below
      List<MessageListener> jsonListener = kafkaBundle.registerMessageHandler(MessageHandlerRegistration
            .<String, SimpleEntity >builder()
            .withDefaultListenerConfig()
            .forTopic(topic)
            .withDefaultConsumer()
            .withValueDeserializer(
                new WrappedNoSerializationErrorDeserializer<>(
                    new KafkaJsonDeserializer<>(new ObjectMapper(), SimpleEntity.class)))
            .withListenerStrategy(new AutocommitMLS<String, SimpleEntity>(jsonHandler, errorHandler))
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
  
  # Admin client is used for checking and creating topics as well as for Health Checks
  adminConfig:
    # Timeout for request to the kafka admin url used by admin clients
    adminClientRequestTimeoutMs: 2000
  
    # Admin Rest Api for accessing the accessing admin functionality
    adminEndpoint:
      - kafka-admin-api-1:9092 
      - kafka-admin-api-2:9092 
  
    # Admin Security information used for all calls against the Admin Rest API
    adminSecurity :
      user: user
      password: password
      protocol: SASL_SSL
    
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
    listenerConfig1:
      # Number of listener instances that will be generated. If > 1, several KafkaConsumer are generated. Kafka assigns these consumers
      # to different partitions of the consumed topic. Number instances should be smaller or equal to the number of partitions.  
      instances: 1
      # If the topic check is configured within the DSL, the listener waits this amount of ms before checking topic existence again. 0 will disable existence check even when configured in DSL
      topicMissingRetryMs: 60000
      # Milliseconds to sleep between two poll intervals if no messages are available
      pollInterval: 200
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
| clientId | Name of the consumer configuration. Sets Kafka's `client.id`. |
| config -> enable.auto.commit | true |
| config -> auto.commit.interval.ms | 1000 |
| config -> auto.offset.reset | earliest |
| config -> key.deserializer | org.apache.kafka.common.serialization.StringDeserializer |
| config -> value.deserializer | org.apache.kafka.common.serialization.StringDeserializer |

#### producers
| Key | Value |
|-----|-------|
| clientId | Name of the producer configuration. Sets Kafka's `client.id`. |
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
| topicMissingRetryMs | 0 |
| pollIntervall | 100 |

## MessageListener
A MessageListener [`MessageListener`](../sda-commons-server-kafka/src/main/java/org/sdase/commons/server/kafka/consumer/MessageListener.java)
is a default poll loop implementation that correctly subscribes for some topics and
includes additional features such as a graceful shutdown when the application stops.

The message listener hands over the received consumer records to a 
[`MessageListenerStrategy`](../sda-commons-server-kafka/src/main/java/org/sdase/commons/server/kafka/consumer/strategies/MessageListenerStrategy.java)
that defines the message handling and the commit behavior. A strategy should use a 
[`MessageHandler`](../sda-commons-server-kafka/src/main/java/org/sdase/commons/server/kafka/consumer/MessageHandler.java) and
a [`ErrorHandler`](../sda-commons-server-kafka/src/main/java/org/sdase/commons/server/kafka/consumer/ErrorHandler.java)
to separate business logic from commit logic as shown e.g. in [`AutocommitStrategy`](../sda-commons-server-kafka/src/main/java/org/sdase/commons/server/kafka/consumer/strategies/autocommit/AutocommitMLS.java) 
to make the strategy reusable
 
### Included MessageListenerStrategies
The bundle provides some [`MessageListenerStrategy`](../sda-commons-server-kafka/src/main/java/org/sdase/commons/server/kafka/consumer/strategies/MessageListenerStrategy.java)
that can be reused in projects.

A strategy is automatically inited with the Prometheus histogram class when using the builder methods. 
You may need to do that explicitly if you use strategies, e.g. in tests. 

#### Autocommit MessageListenerStrategy
This strategy reads messages from the broker and passes the records to a message handler that must be implemented by the user of the bundle. 

The underlying consumer commits records periodically using the kafka config defaults. But, the `MessageListener` does not implement
any extra logic in case of re-balancing. Therefore, the listener does not support an exactly once semantic. It might occur
that messages are redelivered after re-balance activities. 

#### SyncCommit MessageListenerStrategy
This strategy reads messages from the broker and passes the records to a message handler that must be implemented by the user of the bundle.

The strategy requires `enable.auto.commit` set to `false` and uses sync commit explicitly before polling a new chunk.

#### Retry processing error MessageListenerStrategy
This strategy reads messages from the broker and passes the records to a message handler that must be implemented by the user of the bundle. 

The strategy requires `enable.auto.commit` set to `false` and the underlying consumer commits records for each partition. In case of processing errors the 
handler should throw `ProcessingErrorRetryException` which is then delegated to either the self-implemented `ErrorHandler` where finally can be decided if the processing should be stopped or retried (handleError returns any RuntimeException). In case of retry the consumer set the offset on the failing record and interrupt the processing of further records. The next poll will retry the records on this partition starting with the failing record.  

Instead of providing a self-implemented error-handler you can simply don't provide one and the default error handler will be used which simply either returns the thrown RuntimeException or throws a new RuntimeException if there is none occured before. This leads to the wanted, expected, and configured Dead-Letter-Handling.

#### Dead Letter Handling MessageListenerStrategy
This strategy offers a way to retry the processing of failed records, without blocking the running process.
The strategy requires to extra topics for each main topic, one for temporary storing messages for
retry and one for parking messages if retry fails (dead letter).
The message is copied to the retry topic, when the message handler and if existing the error handler also throws an exception.

Messages in the retry topic are copied back to the main topic after a configurable interval for a configurable number of reties.
If retry also fails, the  message is copied to the dead letter topic. From the dead letter topic a process needs to be manually triggered
to put messages from the dead letter topics back to the main topics by a developer. This is implemented via an admin Task.

This strategy reads messages from the broker and passes the records to a message handler that must be implemented by the user of the strategy.

The strategy requires `enable.auto.commit` set to `false` and the underlying consumer commits the records. 

In case of processing errors the handler must throw any `RuntimeException` which is then delegated to the `ErrorHandler` where finally can be decided if
* the processing should be stopped (handleError returns `false`). In case the handleError returns `false` the offset is not being commited and the listener will be stopped.
* the processing continues normally (handleError returns `true`) - e.g. the error could be fixed.
* message will go into the dead letter handling mechanism (handleError again throws a Runtime Exception)

The strategy requires the usage of the `DeadLetterNoSerializationErrorDeserializer`  as a wrapper for key and value deserializer.
If deserialization exception occurs, the message is redirected to the dead letter mechanism.

When adding a message to the retry topic, additional information is being added to the header of the message
* the exception
* the number of retries

##### Example for setting it up in your Application:

```
MessageListenerRegistration.<Pair<String, String>, CareApplication> builder()
            .withDefaultListenerConfig()
            .forTopicConfigs(Collections.singletonList((this.kafkaBundle.getTopicConfiguration("sourceTopicConfigName"))))
            .withConsumerConfig("mainTopicConfigName")
            .withKeyDeserializer(new DeadLetterNoSerializationErrorDeserializer(new SDADeserializer<>((Class<Pair<String, String>>)(Object)Pair.class)))
            .withValueDeserializer(new DeadLetterNoSerializationErrorDeserializer(new SDAFormFieldStringPairDeserializer<>(CareApplication.class)))
            .withListenerStrategy(new DeadLetterMLS(environment, careApplicationService, kafkaBundle, "sourceTopicConsumerConfigName", "sourceTopicConsumerConfigName", 5, 5_000))
            .build();
```

##### Dead Letter Resend Admin Task  
The Admin task name will be: deadLetterResend/"deadLetterTopic", e.g. deadLetterResend/myInputTopic. That means for each strategy that is implemented within the service
a separate admin task will be created to handle multiple strategy usages within a single service. A call is a simple POST, without any parameters, 
e.g. curl -X POST http://localhost:8082/tasks/"Task Name"

##### Naming Conventions for Topic and Consumer/Producer Configurations
All required topics/consumers/producers configuration names are deducted from the source topic name for that the dead-letter strategy is applied.

* TopicConfigurationNames
  * `<main-topic>`  The original topic from that the service normally consumes messages
  * `<main-topic>-retry` The topic where the strategy temporary parks messages until the next retry
  * `<main-topic>-deadLetter` The topic where finally the messages are stored if retry fails
Note, if no configuration for retry and dead letter topics are given, the  convention assumes the following names:
  * `<main-topic>.retry` for retry topic
  * `<main-topic>.deadLetter` for dead letter topic

* ConsumerConfigurationNames
  * `<main-topic>` The consumer that reads messages from `<main-topic>`. The is the consumer in the message listener that uses the DeadLetter strategy
  * `<main-topic>-retry` The consumer that reads messages from the `<main-topic>-retry` topic. This consumer uses the SyncCommit strategy to reinsert messages top the `<main-topic>`
  * `<main-topic>-deadLetter` The consumer that reads messages from the `<main-topic>.deadLetter` topic. This consumer is used in the `DeadLetterTriggerTask` to reinsert parked messages
  
* ProducerConfigurationNames
  * `<main-topic>` The producer that writes messages to the `<main-topic>` topic.
  * `<main-topic>-retry` The producer that writes messages with processing errors to the `<main-topic>.retry` topic.
  * `<main-topic>-deadLetter` The producer that writes messages to `<main-topic>.deadLetter` topic.
  
Example: Original topic is `myTopic`
```yaml
kafka:
  topics:
    myTopic:
      name: myTopic-name
    myTopic-retry:
      name: myTopic-name.retry
    myTopic-deadLetter:
      name: myTopic-name.deadLetter
  consumers:
    myTopic:
      group: defaultConsumer
    myTopic-retry:
      group: defaultRetryConsumer
    myTopic-deadLetter:
      group: defaultDeadLetterConsumer
  producers:
    myTopic-error:
    myTopic-retry:
    myTopic-reinsert:            
```


## Create preconfigured consumers and producers
To give the user more flexibility the bundle allows to create consumers and producers either by name of a valid configuration from the config yaml or 
by specifying a configuration in code. The user takes over the full responsibility and have to ensure that the consumer is closed when not 
longer used.   

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

