# SDA Dropwizard Commons Server Kafka Example

This module presents two very basic example applications
 * one for showing the creation of [`MessageProducer`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/master/sda-commons-server-kafka-example/src/main/java/org/sdase/commons/server/kafka/KafkaExampleProducerApplication.java) and 
 * one for showing the creation of [`MessageListener`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/master/sda-commons-server-kafka-example/src/main/java/org/sdase/commons/server/kafka/KafkaExampleConsumerApplication.java). The consumers are very simple and just store the retrieved values in a list.  

Each of the applications comprises two examples, a simple one and a more complex one using values from the Kafka section of the application configuration.