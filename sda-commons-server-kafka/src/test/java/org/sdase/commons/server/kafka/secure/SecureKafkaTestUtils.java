package org.sdase.commons.server.kafka.secure;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

/** Helpers for test clients which consistently use the secured EXTERNAL listener. */
public final class SecureKafkaTestUtils {
  private static final Duration TIMEOUT = Duration.ofSeconds(30);
  private final SecureKafkaTestResource kafka;

  SecureKafkaTestUtils(SecureKafkaTestResource kafka) {
    this.kafka = kafka;
  }

  public void createTopic(String topic) {
    kafka.createTopic(topic, 1, (short) 1);
  }

  public void createTopic(String topic, int partitions, short replicationFactor) {
    kafka.createTopic(topic, partitions, replicationFactor);
  }

  public <K, V> KafkaProducer<K, V> getKafkaProducer(
      Class<? extends Serializer<K>> keySerializer,
      Class<? extends Serializer<V>> valueSerializer) {
    return kafka.getKafkaProducer(keySerializer, valueSerializer);
  }

  public List<String> getTopicNames() {
    try (Admin admin = Admin.create(kafka.clientProperties())) {
      return admin.listTopics().names().get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS).stream()
          .sorted()
          .toList();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while listing topics", e);
    } catch (Exception e) {
      throw new IllegalStateException("Could not list secure Kafka topics", e);
    }
  }

  public void produceRecords(Map<byte[], byte[]> records, String topic, int partition) {
    List<ProducerRecord<byte[], byte[]>> result = new ArrayList<>();
    records.forEach((key, value) -> result.add(new ProducerRecord<>(topic, partition, key, value)));
    produceRecords(result);
  }

  public void produceRecords(List<ProducerRecord<byte[], byte[]>> records) {
    try (KafkaProducer<byte[], byte[]> producer =
        getKafkaProducer(ByteArraySerializer.class, ByteArraySerializer.class)) {
      for (ProducerRecord<byte[], byte[]> producerRecord : records) {
        producer.send(producerRecord).get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while producing secure test records", e);
    } catch (Exception e) {
      throw new IllegalStateException("Could not produce secure test records", e);
    }
  }

  public List<ConsumerRecord<byte[], byte[]>> consumeAllRecordsFromTopic(String topic) {
    return consumeAllRecordsFromTopic(
        topic, ByteArrayDeserializer.class, ByteArrayDeserializer.class);
  }

  /** Reads up to the end offsets captured at invocation time; no consumer group coordination. */
  public <K, V> List<ConsumerRecord<K, V>> consumeAllRecordsFromTopic(
      String topic,
      Class<? extends Deserializer<K>> keyDeserializer,
      Class<? extends Deserializer<V>> valueDeserializer) {
    Properties p = kafka.clientProperties();
    p.put(ConsumerConfig.GROUP_ID_CONFIG, "sda-secure-" + UUID.randomUUID());
    p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
    p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
    p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);

    try (KafkaConsumer<K, V> consumer = new KafkaConsumer<>(p)) {
      var partitionInfo = consumer.partitionsFor(topic, TIMEOUT);
      if (partitionInfo == null || partitionInfo.isEmpty()) {
        return List.of();
      }
      List<TopicPartition> partitions =
          partitionInfo.stream().map(pi -> new TopicPartition(topic, pi.partition())).toList();
      consumer.assign(partitions);
      consumer.seekToBeginning(partitions);
      Map<TopicPartition, Long> ends = consumer.endOffsets(partitions, TIMEOUT);
      List<ConsumerRecord<K, V>> result = new ArrayList<>();
      long deadline = System.nanoTime() + TIMEOUT.toNanos();
      while (System.nanoTime() < deadline) {
        boolean complete = true;
        for (TopicPartition partition : partitions) {
          if (consumer.position(partition) < ends.get(partition)) {
            complete = false;
            break;
          }
        }
        if (complete) {
          return result;
        }
        consumer.poll(Duration.ofMillis(250)).forEach(result::add);
      }
      throw new IllegalStateException("Timed out consuming secure Kafka topic " + topic);
    } catch (RuntimeException e) {
      throw new IllegalStateException("Could not consume secure Kafka topic " + topic, e);
    }
  }
}
