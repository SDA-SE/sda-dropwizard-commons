# Migration Guide from v9 to v10

## Kafka is upgraded from 3.9.2 to 4.4.0

Kafka is upgraded to 4.4.0 to fix
[CVE-2026-111551](https://kafka.apache.org/community/cve-list/#CVE-2026-111551), which allows
crafted records to exhaust memory in applications using `ListDeserializer`/`ListSerde` with Kafka
clients 3.9.2.

Kafka 4.4.0 removes previously deprecated APIs.
`sda-commons-server-kafka-testing` replaces Salesforce's `kafka-junit5` support with
`KraftKafkaTestResource`, an embedded KRaft cluster without ZooKeeper.
This replacement provides fewer test features and requires changes to existing tests.

For now, the new test resource supports only a single broker.
Multiple brokers, individual broker control, ZooKeeper access, and listener registration are no
longer exposed.
The replacement test utilities also omit several administration, consumer creation, and producer
configuration helpers.

Migration actions:

1. Replace `com.salesforce.kafka.test.junit5.SharedKafkaTestResource` with
   `org.sdase.commons.server.kafka.testing.KraftKafkaTestResource`.
2. Remove `.withBrokers(...)` for single-broker tests. Tests that require replication, failover,
   individual broker restarts, or ZooKeeper need another test fixture. Replace address lookups
   through `getKafkaBrokers()` with `getKafkaConnectString()`; use `KAFKA.stop()` to stop the
   entire embedded cluster when testing an outage.
3. Replace explicit `KafkaTestUtils` types with
   `org.sdase.commons.server.kafka.testing.KraftKafkaTestUtils` and adapt helper calls:

   | Previous usage                                                                    | Migration                                                                                                                            |
   |-----------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
   | `produceRecords(Collection)`                                                      | Pass a `List`. Both retained `produceRecords` overloads return `void`; use a Kafka producer directly when record metadata is needed. |
   | `produceRecords(numberOfRecords, topic, partition)`                               | Construct the records explicitly.                                                                                                    |
   | Partition-selective `consumeAllRecordsFromTopic(...)`                             | Use a Kafka consumer directly. The retained helpers read all partitions.                                                             |
   | `getKafkaConsumer(...)` or `getKafkaProducer(..., Properties)`                    | Create and configure the client directly.                                                                                            |
   | `getAdminClient()`, `describeTopic()`, `getTopics()`, or `describeClusterNodes()` | Use Kafka's `Admin` API directly.                                                                                                    |
   | `waitForBrokerToComeOnLine(...)`                                                  | Implement the readiness check required by the test fixture.                                                                          |

   The new consumption helper stops once it reaches offsets captured at the start of the call.
   Await expected results explicitly when records are produced asynchronously.
4. Replace tests using `registerListener(...)`, including SSL and SASL listeners, with another
   security-capable fixture.
5. Update direct Kafka API calls where applicable:

   | Removed API                                                   | Replacement                                                            |
   |---------------------------------------------------------------|------------------------------------------------------------------------|
   | `consumer.poll(long)`                                         | `consumer.poll(Duration)`                                              |
   | `consumer.committed(TopicPartition)` and its timeout overload | The corresponding `committed(Set<TopicPartition>)` overload            |
   | `producer.sendOffsetsToTransaction(offsets, groupId)`         | `producer.sendOffsetsToTransaction(offsets, consumer.groupMetadata())` |
   | `MockConsumer.setException(...)`                              | `MockConsumer.setPollException(...)`                                   |
   | `Admin.alterConfigs(...)`                                     | `Admin.incrementalAlterConfigs(...)`                                   |

   The producer change also applies to SDA's wrapped producers. Review other direct Kafka API
   usage against the [Kafka upgrade guide](https://kafka.apache.org/44/getting-started/upgrade/).
6. Review producer configuration: Kafka changes the default `linger.ms` from `0` to `5`.
   Set it explicitly if the previous batching behavior is required.
   **Ensure production brokers are version 2.1 or later**.
   Upgrading the client alone does not require migrating an existing compatible broker cluster to
   KRaft.


### Removed Kafka client metrics

Kafka 4.0 removed the deprecated metric names below, so they are also unavailable with the
Kafka 4.4.0 clients used in v10. Replace references in dashboards, alerts, and custom metric
reporters with the direct replacements:

For Micrometer's Prometheus export, update the corresponding names as follows:

| Removed Prometheus series                   | Replacement Prometheus series                  |
|---------------------------------------------|------------------------------------------------|
| `kafka_producer_bufferpool_wait_time_total` | `kafka_producer_bufferpool_wait_time_ns_total` |
| `kafka_producer_io_waittime_total`          | `kafka_producer_io_wait_time_ns_total`         |
| `kafka_producer_iotime_total`               | `kafka_producer_io_time_ns_total`              |
| `kafka_consumer_io_waittime_total`          | `kafka_consumer_io_wait_time_ns_total`         |
| `kafka_consumer_iotime_total`               | `kafka_consumer_io_time_ns_total`              |

See the [Kafka upgrade guide](https://kafka.apache.org/44/getting-started/upgrade/#upgrade_400_notable)
and [KIP-773](https://cwiki.apache.org/confluence/spaces/KAFKA/pages/188744551).

## Golden-file YAML assertions use Jackson 3

`GoldenFileAssertions.hasYamlContentAndUpdateGolden` now uses Jackson 3's `YAMLMapper`
instead of Dropwizard's Jackson 2 mapper. It explicitly uses the YAML 1.2 core schema.
No Dropwizard POJO modules are needed because the assertion compares parsed trees, not Java objects.

Check golden files for these scalar parsing differences:

| YAML input                        | Previous behavior                       | New behavior                               |
|-----------------------------------|-----------------------------------------|--------------------------------------------|
| Unquoted `yes`, `no`, `on`, `off` | Parsed as booleans                      | Parsed as strings                          |
| `value: .nan`                     | Parsing failed with the previous mapper | Accepted as a special floating-point value |

Use `true` and `false` for booleans. Quote values intended as strings. For example,
`value: yes` no longer compares equal to `value: true`, but does compare equal to `value: "yes"`.

Jackson 3 enables trailing-token checks (`FAIL_ON_TRAILING_TOKENS`) by default. Both the golden file
and the expected content must contain a single YAML document. Previously, later documents were
silently ignored, so the assertion could pass despite differences in those documents. Extra
documents
now cause a parsing exception, even when their content matches.

Split multi-document YAML into separate golden files and assert each document separately.
The existing update policy is unchanged: local runs write the complete expected text even when
parsing fails; CI runs never update the golden file. Updating a file alone does not resolve a
multi-document parsing failure.

## AsyncAPI generation uses Jackson 3

AsyncAPI generation in `sda-commons-server-testing` now uses Jackson 3 for YAML parsing,
JSON tree processing, and YAML output.

Update code that consumes `FinalBuilder.generate()` or implements a custom `JsonSchemaBuilder`
to use `tools.jackson.databind.JsonNode` instead of `com.fasterxml.jackson.databind.JsonNode`.
Custom schema builders must return Jackson 3 nodes, including the nodes in their result maps.
Jackson 2 and Jackson 3 node types are not interchangeable.

```java
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
```

`generateYaml()` still returns a `String`. Review regenerated AsyncAPI golden files when upgrading.
Base templates are parsed with Jackson 3's `YAMLMapper`, so the scalar parsing differences described
above also apply. Templates must contain a single YAML document; trailing documents are rejected.
YAML parsing and serialization failures are wrapped in `YamlGenerationException`.

## JSON utilities distinguish Jackson 2 and Jackson 3

The utilities in `org.sdase.commons.server.testing.json` now distinguish the Jackson major version
in their node types:

| Operation                          | Jackson 3 (`tools.jackson.databind`) | Jackson 2 (`com.fasterxml.jackson.databind`) |
|------------------------------------|--------------------------------------|----------------------------------------------|
| Sort object fields in place        | `JsonNodeUtil`                       | `JsonJackson2NodeUtil`                       |
| Rewrite or merge `$ref` references | `RefUtil`                            | `RefJackson2Util`                            |

`JsonNodeUtil` and `RefUtil` now accept Jackson 3 nodes. Update their callers and reference-rewriter
callbacks to use Jackson 3 types, including `StringNode` instead of Jackson 2's `TextNode`.

If you still need Jackson 2 nodes, use `JsonJackson2NodeUtil` or `RefJackson2Util` as a temporary
migration path. Both are deprecated since 10.0.0. They do not convert between Jackson versions.
Migrate the nodes and their callers to Jackson 3 before switching to `JsonNodeUtil` or `RefUtil`.

## AsyncAPI module removed

AsyncAPI generation, formally provided by `sda-commons-shared-asyncapi`, is now integrated in
sda-commons-server-testing.

For migration imports need to be changed:

| Old package (`sda-commons-shared-asyncapi`)             | New package (`sda-commons-server-testing`)             |
|---------------------------------------------------------|--------------------------------------------------------|
| `org.sdase.commons.shared.asyncapi.jsonschema.victools` | `org.sdase.commons.server.testing.jsonschema.victools` |
| `org.sdase.commons.shared.asyncapi.jsonschema`          | `org.sdase.commons.server.testing.jsonschema`          |
| `org.sdase.commons.shared.asyncapi.util`                | `org.sdase.commons.server.testing.json`                |
| `org.sdase.commons.shared.asyncapi`                     | `org.sdase.commons.server.testing.asyncapi`            |
