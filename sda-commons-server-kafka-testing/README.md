# SDA Commons Server Kafka Testing

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-kafka-testing/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-kafka-testing)

The module `sda-commons-server-kafka-testing` is the base module to add unit and integrations test for Kafka broker usage.

It includes the dependencies to [sda-commons-server-testing](../sda-commons-server-testing/README.md) module.

## Use with Kafka-Unit 4

The kafka-junit4 library provides means for easily setting up a Kafka broker that can be reconfigured easily by using the following class rule:
```java
@ClassRule
protected static final SharedKafkaTestResource KAFKA = new SharedKafkaTestResource()
         .withBrokerProperty("auto.create.topics.enable", "false")
         // all avaliable kafka broker properties can be configured
         .withBrokers(2); // number of broker instances in the cluster
```

### Test support with random broker ports
The usage of random ports allows to execute tests in parallel and reduce the probability of port conflicts, e.g. when local-infra is also started.  

The example above starts two Kafka brokers within a cluster. To test your application, you have to configure these servers as 
bootstrap servers. This is normally done via the configuration YAML file within the property `kafka -> brokers`.

You can override these properties programmatically using config overrides when creating your
`DropwizardAppRule`:

```java
private static final DropwizardAppRule<KafkaTestConfiguration> DW =
      new DropwizardAppRule<>(
          KafkaTestApplication.class,
          resourceFilePath("test-config-default.yml"),
          config("kafka.brokers", KAFKA::getKafkaConnectString)
      );
```

## Usage with Kafka-Unit 5

The same things can be done if you prefer JUnit 5. You just have to replace the class rule
by the extension of the same name from package `com.salesforce.kafka.test.junit5` and use 
Dropwizard's app extension. Just make sure you execute the Kafka extension before the Dropwizard extension.

Example:

```java

import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
// ...

class KafkaJUnit5IT {

  @RegisterExtension
  @Order(0) // Start the broker before the app
  static final SharedKafkaTestResource KAFKA = new SharedKafkaTestResource().withBrokers(2);

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<KafkaTestConfiguration> DW =
      new DropwizardAppExtension<>(
          KafkaTestApplication.class,
          resourceFilePath("test-config.yaml"),
          config("kafka.brokers", KAFKA::getKafkaConnectString) // Override the Kafka brokers
      );

}
```