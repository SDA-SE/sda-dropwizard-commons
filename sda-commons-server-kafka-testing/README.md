# SDA Commons Server Kafka Testing

The module `sda-commons-server-kafka-testing` is the base module to add unit and integrations test for kafka broker usage.

It includes the dependencies to the sda-commons-server-testing module, that provides basic test support.

| Group            | Artifact           | Version |
|------------------|--------------------|---------|
| junit            | junit              | 4.12    |
| io.dropwizard    | dropwizard-testing | 1.3.5   |
| org.mockito      | mockito-core       | 2.23.0  |
| org.assertj      | assertj-core       | 3.11.1  |
| com.google.truth | truth              | 0.42    |

For Kafka based tests, the following libraries are included

| Group            | Artifact           | Version |
|------------------|--------------------|---------|
| com.salesforce.kafka.test | kafka-junit4 | 3.0.1 |
| org.apache.kafka | kafka_2.12 | 1.1.1|

The kafka-junit4 library provides means for easily setting up a kafka broker that can be reconfigured easily:
```
protected static final SharedKafkaTestResource KAFKA = new SharedKafkaTestResource()
         .withBrokerProperty("auto.create.topics.enable", "false")
         // all avaliable kafka broker properties can be configured
         .withBrokers(2); // number of broker instances in the cluster
```
The library does not depend on a fixed kafka broker version. This version is given within the second dependency. Thus, 
currently, the kafka version 1.1.1 with scala version 2.12 is used.

An example for setup a test scenario can be found in [KafkaTopicIT.java](./../sda-commons-server-kafka/src/integTest/java/com/sdase/commons/server/kafka/KafkaTopicIT.java)