# SDA Commons Server Kafka Testing

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-kafka-testing/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-kafka-testing)

The module `sda-commons-server-kafka-testing` is the base module to add unit and integrations test for Kafka broker usage.

It includes the dependencies to [sda-commons-server-testing](../sda-commons-server-testing/README.md) module.

For Kafka based tests, the following libraries are included

| Group            | Artifact           | Version |
|------------------|--------------------|---------|
| `com.salesforce.kafka.test` | `kafka-junit4` | 3.0.1 |
| `org.apache.kafka` | `kafka_2.12` | 1.1.1|
| `org.awaitility` | `awaitility` | 3.1.2 |

kafka-junit4 does not depend on a fixed Kafka broker version. The kafka must be included in the used version within an dedicated dependency.
Currently, the Kafka version 1.1.1 with scala version 2.12 is used.

## Usage of Kafka-Unit
The kafka-junit4 library provides means for easily setting up a Kafka broker that can be reconfigured easily by using the following class rule:
```
@ClassRule
protected static final SharedKafkaTestResource KAFKA = new SharedKafkaTestResource()
         .withBrokerProperty("auto.create.topics.enable", "false")
         // all avaliable kafka broker properties can be configured
         .withBrokers(2); // number of broker instances in the cluster
```

## Test support with random broker ports
The usage of random ports allows to execute tests in parallel and reduce the probability of port conflicts, e.g. when local-infra is also started.  

The example above, starts two Kafka brokers within a cluster. To test your application, you have to configure these servers as 
bootstrap servers. This is normally done via the configuration YAML file within the property `kafka -> brokers`.

By using the following snippets, the broker urls are passed to the application.  

**Changes in the YAML:**
```
kafka:
  brokers: ${BROKER_CONNECTION_STRING} 
```

**Usage of `KafkaBrokerEnvironmentRule` within the test**

The `KafkaBrokerEnvironmentRule` sets the broker urls as JSON formatted string into the environment variable:
```
protected static final SharedKafkaTestResource KAFKA = new SharedKafkaTestResource()
         .withBrokers(2);

   protected static final KafkaBrokerEnvironmentRule KAFKA_BROKER_ENVIRONMENT_RULE = new KafkaBrokerEnvironmentRule(KAFKA);

   protected static final DropwizardAppRule<KafkaTestConfiguration> DROPWIZARD_APP_RULE = new DropwizardAppRule<>(
         KafkaTestApplication.class, ResourceHelpers.resourceFilePath("test-config-default.yml"));

   @ClassRule
   public static final TestRule CHAIN = RuleChain.outerRule(KAFKA_BROKER_ENVIRONMENT_RULE).around(DROPWIZARD_APP_RULE); 
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