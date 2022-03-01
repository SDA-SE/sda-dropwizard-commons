# CloudEvents Specification

The SDA-SE uses [CloudEvent specification](https://github.com/cloudevents/spec/) for definition of metadata. We chose to use the [binary format](https://github.com/cloudevents/spec/blob/v1.0.2/cloudevents/bindings/kafka-protocol-binding.md#32-binary-content-mode) to provide backwards capability for events which do not support CloudEvents yet.

By using binary format the metadata gets inserted as headers and not within the message data.

## Async API

Since the specification is pretty straight forward and uses the same structure, we decided to consolidate the structure by defining common traits to reuse within different projects.

There are 2 traits provided, for each supported version:
- one which contains the minimum required metadata
- one which contains no minimum required metadata (for backwards capability)

## Example

The traits can be included within the component definition of the async api by using the GitHub URL to the raw file.

```
asyncapi: '2.0.0'

info:
  title: The specification title
  description: The specification description
  version: 1.0.0
  contact:
    name: SDA SE - Team Keystone
    email: philipp.euwen@sda.se
  license:
    name: SDA SE Non Public License
    url: https://www.sda.se
  termsOfService: https://sda.se/nutzungsbedingungen/

channels:
  'events-topic':
    subscribe:
      summary: The topic summary
      description: The topic description
      message:
        oneOf:
          - $ref: '#/components/messages/KafkaEvent'
          
components:
  messages:
    KafkaEvent:
      title: The event
      description: The event description
      bindings:
        kafka:
          key:
            type: string
            description: Timestamp of event as milliseconds since 1st Jan 1970
      traits:
        - $ref: "https://raw.githubusercontent.com/SDA-SE/sda-dropwizard-commons/master/sda-commons-shared-asyncapi/cloudevents/v1.0.2/asyncapi-trait.yml"
      payload:
        $ref: './kafkaEvent.json#'
```