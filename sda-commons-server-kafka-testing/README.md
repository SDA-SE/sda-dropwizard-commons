# SDA Commons Server Kafka Testing

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-kafka-testing/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-kafka-testing)

The module `sda-commons-server-kafka-testing` is the base module to add unit and integrations test 
for Kafka broker usage.

It includes the dependencies to [sda-commons-server-testing](../sda-commons-server-testing/README.md) module.

## Usage of Kafka-JUnit 5

The kafka-junit5 library provides means for easily setting up a Kafka broker that can be 
reconfigured easily by using the following extension:
```
@RegisterExtension
public static final SharedKafkaTestResource KAFKA = new SharedKafkaTestResource()
         .withBrokerProperty("auto.create.topics.enable", "false")
         // all avaliable kafka broker properties can be configured
         .withBrokers(2); // number of broker instances in the cluster
```

## Test support with random broker ports
The usage of random ports allows to execute tests in parallel and reduce the probability of port 
conflicts, e.g. when local-infra is also started.  

The example above, starts two Kafka brokers within a cluster. To test your application, you have to 
configure these servers as bootstrap servers. This is normally done via the `config.yaml` file
setting the property `kafka.brokers`.

By using the following snippets, the broker urls are passed to the application.  

**Changes in the YAML:**
```
kafka:
  brokers: ${BROKER_CONNECTION_STRING} 
```

**Usage of `KafkaBrokerEnvironmentExtension` within the test**

The `KafkaBrokerEnvironmentExtension` sets the broker urls as JSON formatted string into the 
environment variable:

```
  @RegisterExtension
  public static final KafkaBrokerEnvironmentExtension KAFKA =
      new KafkaBrokerEnvironmentExtension(new SharedKafkaTestResource().withBrokers(2)); 
```

**Usage of ConfigurationSubstitutionBundle within the application**

This requires also to add the `ConfigurationSubstitutionBundle` to your bundle.
```
bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build()); 
```

**Example content of the environment variable**
```
[ "127.0.0.1:38185", "127.0.0.1:44401" ]
```
An example for setup a test scenario can be found in [KafkaBundleWithConfigIT.java](./../sda-commons-server-kafka/src/test/java/org/sdase/commons/server/kafka/KafkaBundleWithConfigIT.java)