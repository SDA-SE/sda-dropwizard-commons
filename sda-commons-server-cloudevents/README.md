# SDA Commons Server CloudEvents

Provides some glue code to work with [CloudEvents](https://cloudevents.io/) on top of Apache Kafka.

> #### ⚠️ Experimental ⚠
>
> The bundle is still in an early state. APIs are open for change.
>

## Introduction

CloudEvents is a general standard that can be used in combination with your favourite eventing tool
like ActiveMQ or Kafka. The CloudEvents specification defines concrete bindings to define how the
general specification should be applied to a specific tool. This module brings you
the [Kafka protocol binding](https://github.com/cloudevents/spec/blob/v1.0.2/cloudevents/bindings/kafka-protocol-binding.md)
.

The following code examples will show you how you can use this bundle in combination with our Kafka
bundle.

## Add Dropwizard bundle

This module provides a small Dropwizard bundle that helps you creating helper classes for consuming
and producing CloudEvents. It will mainly re-use the application's `ObjectMapper` for (de)
serializing your objects as JSON.

```java
    private final CloudEventsBundle<CloudEventsTestConfig> cloudEventsBundle =
      CloudEventsBundle.<CloudEventsTestConfig>builder().build();

    ...
    @Override
    public void initialize(Bootstrap<CloudEventsTestConfig> bootstrap) {
      ...
      bootstrap.addBundle(kafkaBundle);
      bootstrap.addBundle(cloudEventsBundle);
    }

```

## Producing CloudEvents

CloudEvents gives you two possibilities to encode your CloudEvents:

* [BINARY content mode](https://github.com/cloudevents/spec/blob/v1.0.2/cloudevents/bindings/kafka-protocol-binding.md#32-binary-content-mode)
  The binary content mode seperated event metadata and data; data will be put into the value of your
  Kafka record; metadata will be put into the headers
* [STRUCTURED content mode](https://github.com/cloudevents/spec/blob/v1.0.2/cloudevents/bindings/kafka-protocol-binding.md#33-structured-content-mode)
  The structured content mode keeps event metadata and data together in the payload, allowing simple
  forwarding of the same event across multiple routing hops, and across multiple protocols.

We suggest using the binary content mode. When you use the `CloudEventSerializer` without any
constructor parameters you will use the binary content mode.

```java
    MessageProducer<String, CloudEvent> partnerCreatedMessageProducer =
    kafkaBundle.registerProducer(
    ProducerRegistration.<String, CloudEvent>builder()
    .forTopic(kafkaBundle.getTopicConfiguration("partner-created"))
    .withProducerConfig("partner-created")

    // Use serializer for CloudEvents
    .withValueSerializer(new CloudEventSerializer())

    .build());
```

## Consuming CloudEvents

If you're using the BINARY content mode as suggested you have two options here:

* consume CloudEvents and unrwap your business event using our `CloudEventsConsumerHelper`
* directory consume your business event using a standard `KafkaJsonDeserializer`.

### Consume CloudEvents

__Setup in your Application__

```java
    // Use helper class for unwrapping the business event
    CloudEventsConsumerHelper ceConsumer=cloudEventsBundle.createCloudEventConsumerHelper();

    // Your message handler
    ContractCreatedMessageHandler contractCreatedMessageHandler=
    new ContractCreatedMessageHandler(ceConsumer,inMemoryStore);

    // Define the message listener
    kafkaBundle.createMessageListener(
    MessageListenerRegistration.builder()
      .withListenerConfig("contract-created")
      .forTopicConfigs(
      Collections.singleton(kafkaBundle.getTopicConfiguration("contract-created")))
      .withConsumerConfig("contract-created")
      .withKeyDeserializer(new StringDeserializer())

      // Use deseriallizer for CloudEvents
      .withValueDeserializer(new CloudEventDeserializer())

      .withListenerStrategy(
        new AutocommitMLS<>(contractCreatedMessageHandler,contractCreatedMessageHandler))
      .build());
```

__Process the events in your service__

```java
  @Override
  public void handle(ConsumerRecord<String, CloudEvent> record) {
    try {
      // Unwrap your business event
      ContractCreatedEvent event=ceConsumer.unwrap(record.value(),ContractCreatedEvent.class);
  
      // Do something with it
      LOGGER.info(
        "Contract {} created for partner {}",event.getContractId(),event.getPartnerId());
      } catch(IOException e){
        LOGGER.error("Can't read CloudEvent's data",e);
      }
    }
```