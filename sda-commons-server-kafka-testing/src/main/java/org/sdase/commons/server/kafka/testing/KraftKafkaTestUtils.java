package org.sdase.commons.server.kafka.testing;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

public class KraftKafkaTestUtils {
  private static final Duration TIMEOUT = Duration.ofSeconds(20);
  private final String bootstrapServers;

  KraftKafkaTestUtils(String bootstrapServers) {
    this.bootstrapServers = bootstrapServers;
  }

  public void createTopic(String topic, int partitions, short replicationFactor) {
    Properties properties = new Properties();
    properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

    try (Admin admin = Admin.create(properties)) {
      admin
          .createTopics(List.of(new NewTopic(topic, partitions, replicationFactor)))
          .all()
          .get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while creating Kafka topic " + topic, e);
    } catch (ExecutionException e) {
      if (!(e.getCause() instanceof TopicExistsException)) {
        throw new IllegalStateException("Could not create Kafka topic " + topic, e);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Could not create Kafka topic " + topic, e);
    }
  }

  public Set<String> getTopicNames() {
    Properties properties = new Properties();
    properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

    try (Admin admin = Admin.create(properties)) {
      return admin.listTopics().names().get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while listing Kafka topics", e);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to retrieve Kafka topic names", e);
    }
  }

  public <K, V> KafkaProducer<K, V> getKafkaProducer(
      Class<? extends Serializer<K>> keySerializer,
      Class<? extends Serializer<V>> valueSerializer) {

    Properties properties = new Properties();
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
    properties.put(ProducerConfig.ACKS_CONFIG, "all");

    return new KafkaProducer<>(properties);
  }

  public <K, V> KafkaConsumer<K, V> getKafkaConsumer(
      Class<? extends Deserializer<K>> keyDeserializer,
      Class<? extends Deserializer<V>> valueDeserializer,
      Properties additionalProperties) {
    Properties properties = new Properties();
    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);

    if (additionalProperties != null) {
      properties.putAll(additionalProperties);
    }

    return new KafkaConsumer<>(properties);
  }

  public void produceRecords(List<ProducerRecord<byte[], byte[]>> records) {
    try (KafkaProducer<byte[], byte[]> producer =
        getKafkaProducer(ByteArraySerializer.class, ByteArraySerializer.class)) {

      for (ProducerRecord<byte[], byte[]> record : records) {
        producer.send(record).get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while producing records", e);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to produce records", e);
    }
  }

  public void produceRecords(Map<byte[], byte[]> records, String topic, int partition) {

    Properties properties = new Properties();
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
    properties.put(
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
    properties.put(ProducerConfig.ACKS_CONFIG, "all");

    try (KafkaProducer<byte[], byte[]> producer = new KafkaProducer<>(properties)) {

      for (Map.Entry<byte[], byte[]> record : records.entrySet()) {
        producer
            .send(new ProducerRecord<>(topic, partition, record.getKey(), record.getValue()))
            .get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while producing Kafka records", e);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to produce Kafka records", e);
    }
  }

  public List<ConsumerRecord<byte[], byte[]>> consumeAllRecordsFromTopic(String topic) {

    return consumeAllRecordsFromTopic(
        topic, ByteArrayDeserializer.class, ByteArrayDeserializer.class);
  }

  public <K, V> List<ConsumerRecord<K, V>> consumeAllRecordsFromTopic(
      String topic,
      Class<? extends Deserializer<K>> keyDeserializer,
      Class<? extends Deserializer<V>> valueDeserializer) {

    Properties properties = new Properties();
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, "sda-test-" + UUID.randomUUID());
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

    try (KafkaConsumer<K, V> consumer =
        getKafkaConsumer(keyDeserializer, valueDeserializer, properties)) {
      var partitionInfo = consumer.partitionsFor(topic, TIMEOUT);

      if (partitionInfo == null || partitionInfo.isEmpty()) {
        return List.of();
      }

      List<TopicPartition> partitions =
          partitionInfo.stream().map(p -> new TopicPartition(topic, p.partition())).toList();

      consumer.assign(partitions);
      consumer.seekToBeginning(partitions);

      Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions, TIMEOUT);

      List<ConsumerRecord<K, V>> results = new ArrayList<>();
      long deadline = System.nanoTime() + TIMEOUT.toNanos();

      while (System.nanoTime() < deadline) {
        boolean finished =
            partitions.stream()
                .allMatch(p -> consumer.position(p, Duration.ofSeconds(5)) >= endOffsets.get(p));

        if (finished) {
          return results;
        }

        consumer.poll(Duration.ofMillis(250)).forEach(results::add);
      }

      throw new IllegalStateException("Timed out consuming topic: " + topic);
    }
  }
}
