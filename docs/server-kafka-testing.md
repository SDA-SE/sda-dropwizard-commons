# SDA Commons Server Kafka Testing

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-kafka-testing/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-kafka-testing)

The `sda-commons-server-kafka-testing` module provides utilities for unit and integration testing of Kafka-based applications.

It includes the dependencies from the [sda-commons-server-testing](./server-testing.md) module.

The `KraftKafkaTestResource` JUnit 5 extension starts an embedded Kafka cluster using KRaft. Neither ZooKeeper nor Docker is required.

```java
@RegisterExtension
@Order(0) // Start Kafka before the application
static final KraftKafkaTestResource KAFKA =
    new KraftKafkaTestResource();
```

## Test support with random broker ports

The embedded brokers use random ports to avoid conflicts with other services.

Configure the application to use the embedded cluster by overriding `kafka.brokers` in the `DropwizardAppExtension`:

```java
@RegisterExtension
@Order(0)
static final KraftKafkaTestResource KAFKA =
    new KraftKafkaTestResource();

@RegisterExtension
@Order(1)
static final DropwizardAppExtension<KafkaTestConfiguration> DW =
    new DropwizardAppExtension<>(
        KafkaTestApplication.class,
        resourceFilePath("test-config.yaml"),
        config("kafka.brokers", KAFKA::getKafkaConnectString));
```

## Create topics

Use `CreateKafkaTopicsClassExtension` to create topics before starting the application.

```java
@RegisterExtension
@Order(0)
static final KraftKafkaTestResource KAFKA =
    new KraftKafkaTestResource();

@RegisterExtension
@Order(1)
static final CreateKafkaTopicsClassExtension TOPICS =
    new CreateKafkaTopicsClassExtension(KAFKA, Arrays.asList("foo", "bar"));

@RegisterExtension
@Order(2)
static final DropwizardAppExtension<KafkaExampleConfiguration> DW =
    new DropwizardAppExtension<>(
        KafkaExampleProducerApplication.class,
        resourceFilePath("test-config-producer.yml"),
        config("kafka.brokers", KAFKA::getKafkaConnectString));
```

## Security testing

For SSL and SASL integration tests, use `SecureKafkaTestResource`.

It supports SSL, SASL/PLAIN and SASL/SCRAM-SHA-512 using an embedded KRaft cluster.