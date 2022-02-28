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

## Producing CloudEvents

CloudEvents gives you two possibilities to encode your CloudEvents:

* [BINARY content mode](https://github.com/cloudevents/spec/blob/v1.0.2/cloudevents/bindings/kafka-protocol-binding.md#32-binary-content-mode)
  The binary content mode seperated event metadata and data; data will be put into the value of your
  Kafka record; metadata will be put into the headers
* [STRUCTURED content mode](https://github.com/cloudevents/spec/blob/v1.0.2/cloudevents/bindings/kafka-protocol-binding.md#33-structured-content-mode)
  The structured content mode keeps event metadata and data together in the payload, allowing simple
  forwarding of the same event across multiple routing hops, and across multiple protocols.

We suggest using the **structured** content mode. It is very explicit and easy to use because you get
the whole event as a simple POJO class. 

For simplicity, we recommend extending our base class: 
```java
import org.sdase.commons.server.cloudevents.CloudEventV1;

public class PartnerCreatedEvent extends CloudEventV1<PartnerCreatedEvent.PartnerCreated> {

  public static class PartnerCreated {
    private String id;

    public String getId() {
      return id;
    }

    public PartnerCreated setId(String id) {
      this.id = id;
      return this;
    }
  }
}
```

You can simply use a standard `KafkaJsonSerializer` to publish the event. 

Take a look at the source code of the examples in directory `src/test/java/.../app` of this module
to get some ideas how you can work with CloudEvents.

## Consuming CloudEvents

You can easily consume the CloudeEvent using a standard `KafkaJsonDeserializer`.