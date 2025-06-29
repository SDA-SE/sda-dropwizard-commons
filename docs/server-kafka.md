# SDA Dropwizard Commons Server Kafka

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-kafka/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-kafka)

This module provides a [`KafkaBundle`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-server-kafka/src/main/java/org/sdase/commons/server/kafka/KafkaBundle.java) adds convenient
functionality to create Kafka consumers, producers, and topics via configuration or Java DSL.

It additionally provides a default [`MessageListener`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-server-kafka/src/main/java/org/sdase/commons/server/kafka/consumer/MessageListener.java) that
implements a polling loop for Kafka consumers. The user of this bundle must only implement the functional logic.


## Usage

Add the following dependency:
```groovy
compile 'org.sdase.commons:sda-commons-server-kafka:<current-version>'
```

Add the Kafka Configuration to your configuration class:
```java

public class YourConfiguration extends SdaPlatformConfiguration {
  // ...

  @NotNull @Valid private KafkaConfiguration kafka = new KafkaConfiguration();

  // ...
}
```

Bootstrap the bundle (see below).

**Configuration Summary**

The following configuration can be used as base for a production-ready configuration.

Not every service both _consumes_ and _produces_, so the properties for `producers`, `consumers`, or `listenerConfig` can be removed if they don't apply to the service.
Other defaults can be added, if needed by the service.

```yaml
kafka:
  # List of brokers to bootstrap consumer and provider connection.
  brokers: ${KAFKA_BROKERS:-[]}

  # Security information used for all consumers and providers to connect to kafka.
  security:
    user: "${KAFKA_SECURITY_USER}"
    password: "${KAFKA_SECURITY_PASSWORD}"
    protocol: ${KAFKA_SECURITY_PROTOCOL:-PLAINTEXT}
    saslMechanism: ${KAFKA_SECURITY_SASL_MECHANISM:-PLAIN}

  # Map with topic configurations. The key is the used as name to address the configuration within the code.
  topics:
    <your-topic-config-key>:
      name: ${KAFKA_TOPICS_<YOUR_CONFIG>_NAME:-default}

  # Map with producer configurations. The key is used as name to address the configuration within the code.
  # Can be removed if no producers are used.
  producers:
    <your-producer-config-key>:
      # Empty by default. Can be overridden by System Properties, so it must be part of this configuration.

  # Map with consumer configurations. The key is used as name/id to address the configuration within the code.
  # Can be removed if no consumers are used.
  consumers:
    <your-consumer-config-key>:
      group: ${KAFKA_CONSUMERS_<YOUR_CONFIG>_GROUP:-default}

  # Map with listener configurations that can be used within MessageListener creation.
  listenerConfig:
    <your-listener-config-key>:
      # Empty by default. Can be overridden by System Properties, so it must be part of this configuration.
```

A possible documentation could look like this:

```md
## Environment Variables

The following environment variables can be used to configure the Docker container:

// ...

### Kafka

A short overview about what kind of messages the service consumes or produces.

The following properties can be set:

#### Broker connection

* `KAFKA_BROKERS` _array_
  * The list of Kafka brokers
  * Example: `["kafka-broker1:9092", "kafka-broker2:9092"]`
  * Default: `[]`
* `KAFKA_SECURITY_USER` _string_
  * The username used to connect to Kafka.
  * Example: `""`
* `KAFKA_SECURITY_PASSWORD` _string_
  * The password used to connect to Kafka.
  * Example: `""`
* `KAFKA_SECURITY_PROTOCOL` _string_
  * The security protocol used by Kafka.
  * Example: `PLAINTEXT` or `SASL_SSL`
  * Default: `PLAINTEXT`
* `KAFKA_SECURITY_SASL_MECHANISM` _string_
  * The SASL mechanism to use to connect to the Kafka.
  * Example: `PLAIN, SCRAM-SHA-256, or SCRAM-SHA-512`
  * Default: `PLAIN`

#### Topics
* `KAFKA_TOPICS_<YOUR_CONFIG>_NAME` _string_
  * Topic name where to read medical record updates. It is checked that the topic is configured in "compact" mode.
  * Example: `medicalRecordsTopic`
  * Default: `default`

#### Consumers
* `KAFKA_CONSUMERS_<YOUR_CONFIG>_GROUP` _string_
  * Consumer group name for the consumer `YOUR_CONFIG`.
  * Example: `myConsumer`
  * Default: `default`
```

**Bootstrap**

The bundle got enhanced to allow more control and flexibility how Kafka messages are consumed and which commit strategy is used.

The bundle should be added as field to the application since it provides methods for the creation of `MessageProducer` and `MessageListener`.
The Builders for `MessageListenerRegistration` and `ProducerRegistration` supports the user in the creation of these complex configurable objects.

```java
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
      MessageHandler<String, String> handler = record -> results.add(record.value());
      ErrorHandler<Sting, String> errorHandler = new IgnoreAndProceedErrorHandler<>();

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
                  .forTopic(TopicConfig.builder("mytopic").build())
                  .withProducerConfig("producer1") // use producer config from config yaml
                  .build());

      // JSON Example
      MessageHandler<String, SimpleEntity> jsonHandler = record -> jsonResults.add(record.value());
      ErrorHandler<Sting, SimpleEntity> errorHandler = new IgnoreAndProceedErrorHandler<>();

      List<MessageListener> jsonListener = kafkaBundle
            .createMessageListener(MessageListenerRegistration.builder()
                  .withDefaultListenerConfig()
                  .forTopic(topic)
                  .withDefaultConsumer()
                  .withValueDeserializer(new KafkaJsonDeserializer<>(new ObjectMapper(), SimpleEntity.class))
                  .withListenerStrategy(new AutocommitMLS<String, SimpleEntity>(jsonHandler, errorHandler))
                  .build()
            );

      MessageProducer<String, SimpleEntity> jsonProducer = kafkaBundle.registerProducer(ProducerRegistration
            .builder()
            .forTopic(topic)
            .withDefaultProducer()
            .withKeySerializer(new StringSerializer())
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

### Connect to multiple Kafka Cluster in one application

To connect to multiple Kafka Clusters in one application, one `KafkaBundle` for each Cluster must be
used.
A dedicated configuration for each bundle is needed as well.
Only in this case, the name of the health check for each bundle must be customized.

```java
public class YourConfiguration extends SdaPlatformConfiguration {
  private KafkaConfiguration kafkaExternal = new KafkaConfiguration();
  private KafkaConfiguration kafkaInternal = new KafkaConfiguration();

  // ...
}
```

```java
public class DemoApplication {
   private final KafkaBundle<YourConfiguration> kafkaBundleExternal = 
       KafkaBundle
           .builder()
           .withConfigurationProvider(YourConfiguration::getKafkaExternal)
           .withHealthCheckName("kafkaConnectionExternal")
           .build();
   
   private final KafkaBundle<YourConfiguration> kafkaBundleInternal = 
       KafkaBundle
           .builder()
           .withConfigurationProvider(YourConfiguration::getKafkaInternal)
           .withHealthCheckName("kafkaConnectionInternal")
           .build();

   public void initialize(Bootstrap<AppConfiguration> bootstrap) {
      // ...
      bootstrap.addBundle(kafkaBundleExternal);
      bootstrap.addBundle(kafkaBundleInternal);
   }

   public void run(AppConfiguration configuration, Environment environment) throws Exception {
       // ...
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

```java
public class DemoApplication {

  private final KafkaBundle<AppConfiguration> kafkaBundle = KafkaBundle.builder()
      .withConfigurationProvider(AppConfiguration::getKafka).build();
  private final MessageProducer<String, ProductBundle> producer;

  public void initialize(Bootstrap<AppConfiguration> bootstrap) {
    bootstrap.addBundle(kafkaBundle);
  }

  public void run(AppConfiguration configuration, Environment environment) throws Exception {
    // register with default consumer and listener config
    // The handler implements the actual logic for the message processing
    // ...

    // JSON Example with wrapped Deserializer to avoid DeseriliazationException, see Note below
    List<MessageListener> jsonListener = kafkaBundle.registerMessageHandler(
        MessageHandlerRegistration
            .builder()
            .withDefaultListenerConfig()
            .forTopic(topic)
            .withDefaultConsumer()
            .withValueDeserializer(
                new WrappedNoSerializationErrorDeserializer<>(
                    new KafkaJsonDeserializer<>(new ObjectMapper(), SimpleEntity.class)))
            .withListenerStrategy(
                new AutocommitMLS<String, SimpleEntity>(jsonHandler, errorHandler))
            .build()
    );
  }
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
    saslMechanism: PLAIN

  # Additional configuration properties that are added to all consumers, producers, and the admin client
  # configuration key -> values as defined in the kafka documentation
  config:
    ssl.truststore.location: /my/truststore/location.jks

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
  topics:
    # id of the topic configuration
    topic1: # topic key
      # topic name
      name: topic1-name
    topic2:
      name: topic2-name
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
      # Number of retries in case a Retry processing error MessageListenerStrategy is chosen. If no value is given, the default is to retry infinitely
      maxRetries: 5
```

You can disable the  health check manually if Kafka is not essential for the functionality of your service,
e.g. it's only used to invalidate a cache, notify about updates or other minor tasks that could fail without
affecting the service so much it's no longer providing a viable functionality.

This way your service can stay healthy even if the connection to Kafka is disrupted.

```java
private KafkaBundle<KafkaTestConfiguration> bundle =
  KafkaBundle.builder()
    .withConfigurationProvider(KafkaTestConfiguration::getKafka)
    .withoutHealthCheck() // disable kafkas health check
    .build();
```
Keep in mind that in this case a producer might fail if the broker is not available, so depending on the
use case the producer should do appropriate error handling e.g. storing unprocessed messages in a queue until the broker is available again.
If no error handling is done you might be better off not disabling the health check.

Disabling the internal health check registers an external health check for monitoring purposes
and to determine the fitness of the service. The connection to the broker can be monitored through
Prometheus metrics without impacting the health of the service.

### Security Settings

There are different configuration options to connect to a Kafka Broker.

#### PLAINTEXT

The server uses an unencrypted connection with no authentication.

```yaml
  security :
    protocol: PLAINTEXT # can be omitted, default value
```

#### SSL

The server uses an encrypted connection with no authentication.

```yaml
  security :
    protocol: SSL
```

#### SASL_PLAINTEXT

The server uses an unencrypted connection with `PLAIN` authentication.

```yaml
  security :
    protocol: SASL_PLAINTEXT
    saslMechanism: PLAIN # can be omitted, default value when username and password are specified
    user: user
    password: password
```

#### SASL_SSL

The server uses an encrypted connection with `PLAIN` authentication.

```yaml
  security :
    protocol: SASL_SSL
    saslMechanism: PLAIN # can be omitted, default value when username and password are specified
    user: user
    password: password
```

#### Other SASL mechanisms

Beside `sasl.mechanism: PLAIN`, the bundle also supports `SCRAM-SHA-256` and `SCRAM-SHA-512`.
All mechanisms can be used with both `SASL_PLAINTEXT` and `SASL_SSL`.

```yaml
  security :
    protocol: SASL_PLAINTEXT # or SASL_SSL
    saslMechanism: SCRAM-SHA-512 # or SCRAM-SHA-256
    user: user
    password: password
```

Further authentication mechanisms can be configured by setting the original Kafka config properties.
The SDA Commons specific configuration of `security.saslMechanism` must be omitted in this case,
because `security.saslMechanism` will only accept the values documented above.
The original Kafka config properties will be used to overwrite the defaults of the SDA Commons Kafka
security configuration before the configuration is applied.

??? example "Example configuration for `OAUTHBEARER`"
    ```yaml
    --8<-- "sda-commons-server-kafka/src/test/resources/config/given/kafka-bearer-custom.yaml"
    ```

#### Per Connection Configuration

Properties can also be configured individually for each consumer, each producer, and the admin
client.

??? example "Individual configuration for consumers, producers and admin client"
    ```yaml
      consumers:
        yourConsumer:
          config:
            sasl.mechanism: OTHER-VALUE
            sasl.jaas.config: "org.apache.kafka.common.security.scram.ScramLoginModule required username='youruser' password='yourpassword';"
    
      producers:
        yourProducer:
          config:
            sasl.mechanism: OTHER-VALUE
            sasl.jaas.config: "org.apache.kafka.common.security.scram.ScramLoginModule required username='youruser' password='yourpassword';"
    
      adminConfig:
        config:
          sasl.mechanism: OTHER-VALUE
          sasl.jaas.config: "org.apache.kafka.common.security.scram.ScramLoginModule required username='youruser' password='yourpassword';"
    ```

#### Custom Certificate Authority and Client Certificates

`SSL` or `SASL_SSL` can also use Kafka brokers that have a self-signed or private-CA certificate. 
Use the Java-default system properties `javax.net.ssl.trustStore` and `javax.net.ssl.trustStorePassword` to provide the certificates (see [`KafkaBundleWithSslTruststoreIT`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-server-kafka/src/test/java/org/sdase/commons/server/kafka/KafkaBundleWithSslTruststoreIT.java)).

For more control, configure the truststore only for the Kafka bundle and not for the complete JVM (see [`KafkaBundleWithSslIT`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-server-kafka/src/test/java/org/sdase/commons/server/kafka/KafkaBundleWithSslIT.java)): 

```yaml
  security :
    protocol: SSL

  config:
    ssl.truststore.location: /your/truststore/location.jks
    ssl.truststore.password: truststore-password
```

This configuration option also supports providing client certificates in a custom keystore:

```yaml
  security :
    protocol: SSL

  config:
    # configure the truststore
    ssl.truststore.location: /your/truststore/location.jks
    ssl.truststore.password: truststore-password

    # configure the keystore with client-certificates
    ssl.keystore.location: /your/keystore/location.jks
    ssl.keystore.password: keystore-password
    ssl.key.password: cert-key-password
```

### Configuration value defaults (extending/changing the Kafka defaults)
These are only the defaults that are explicitly set within the code of the bundle. All other properties depends on the actual broker configuration or the Kafka defaults are used.

| Key                         | Value |
|-----------------------------|-------|
| disabled                    | false |
| adminClientRequestTimeoutMs | 5000  |

#### brokers
No defaults

#### security

| Key      | Value     |
|----------|-----------|
| protocol | PLAINTEXT |

#### consumers

| Key                               | Value                                                         |
|-----------------------------------|---------------------------------------------------------------|
| group                             | default                                                       |
| clientId                          | Name of the consumer configuration. Sets Kafka's `client.id`. |
| config -> enable.auto.commit      | true                                                          |
| config -> auto.commit.interval.ms | 1000                                                          |
| config -> auto.offset.reset       | earliest                                                      |
| config -> key.deserializer        | org.apache.kafka.common.serialization.StringDeserializer      |
| config -> value.deserializer      | org.apache.kafka.common.serialization.StringDeserializer      |

#### producers

| Key                        | Value                                                         |
|----------------------------|---------------------------------------------------------------|
| clientId                   | Name of the producer configuration. Sets Kafka's `client.id`. |
| config -> acks             | all                                                           |
| config -> retries          | 0                                                             |
| config -> linger.ms        | 0                                                             |
| config -> key.serializer   | org.apache.kafka.common.serialization.StringSerializer        |
| config -> value.serializer | org.apache.kafka.common.serialization.StringSerializer        |

#### listenerConfig

| Key                 | Value          |
|---------------------|----------------|
| instances           | 1              |
| topicMissingRetryMs | 0              |
| pollInterval        | 100            |
| maxRetries          | Long.MAX_VALUE |

## MessageListener
A MessageListener [`MessageListener`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-server-kafka/src/main/java/org/sdase/commons/server/kafka/consumer/MessageListener.java)
is a default poll loop implementation that correctly subscribes for some topics and
includes additional features such as a graceful shutdown when the application stops.

The message listener hands over the received consumer records to a
[`MessageListenerStrategy`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-server-kafka/src/main/java/org/sdase/commons/server/kafka/consumer/strategies/MessageListenerStrategy.java)
that defines the message handling and the commit behavior. A strategy should use a
[`MessageHandler`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-server-kafka/src/main/java/org/sdase/commons/server/kafka/consumer/MessageHandler.java) and
a [`ErrorHandler`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-server-kafka/src/main/java/org/sdase/commons/server/kafka/consumer/ErrorHandler.java)
to separate business logic from commit logic as shown e.g. in [`AutocommitStrategy`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-server-kafka/src/main/java/org/sdase/commons/server/kafka/consumer/strategies/autocommit/AutocommitMLS.java)
to make the strategy reusable

### Included MessageListenerStrategies
The bundle provides some [`MessageListenerStrategy`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-server-kafka/src/main/java/org/sdase/commons/server/kafka/consumer/strategies/MessageListenerStrategy.java)
that can be reused in projects.

A strategy is automatically inited with the Prometheus histogram class when using the builder methods.
You may need to do that explicitly if you use strategies, e.g. in tests.

#### Autocommit MessageListenerStrategy
This strategy reads messages from the broker and passes the records to a message handler that must be implemented by the user of the bundle.

The underlying consumer commits records periodically using the kafka config defaults. But, the `MessageListener` does not implement
any extra logic in case of re-balancing. Therefore, the listener does not support an exact once semantic. It might occur
that messages are redelivered after re-balance activities.

#### SyncCommit MessageListenerStrategy
This strategy reads messages from the broker and passes the records to a message handler that must be implemented by the user of the bundle.

The strategy requires `enable.auto.commit` set to `false` and uses sync commit explicitly before polling a new chunk.

#### Retry processing error MessageListenerStrategy
This strategy reads messages from the broker and passes the records to a message handler that must be implemented by the user of the bundle.

The strategy requires `enable.auto.commit` set to `false` and the underlying consumer commits records for each partition. In case of processing errors the
handler should throw `ProcessingErrorRetryException` which is then delegated to the `ErrorHandler` where finally can be decided if the processing should be
stopped or retried (handleError returns `false`). In case of retry the consumer set the offset on the failing record and interrupt the processing of further
records. The next poll will retry the records on this partition starting with the failing record.

## Create preconfigured consumers and producers
To give the user more flexibility the bundle allows to create consumers and producers either by name of a valid configuration from the config YAML or
by specifying a configuration in code. The user takes over the full responsibility and have to ensure that the consumer is closed when no
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

In this case, the `KAFKA_BROKERS` variable should contain a JSON array with a list of broker

```json
[
  "kafka-broker:12345",
  "kafka-broker:54321"
]
```

## Health check

A health check with the name kafkaConnection is automatically registered to test the Kafka connection. The health check tries to list the topics available at the broker.

## Testing
[`sda-commons-server-kafka-testing`](./server-kafka-testing.md) provides support for integration testing with Kafka with JUnit 4.

## Eventing

If you want to use Kafka in the context of eventing when consuming or producing messages, you 
might check out our module [`sda-commons-server-cloudevents`](./server-cloud-events.md).