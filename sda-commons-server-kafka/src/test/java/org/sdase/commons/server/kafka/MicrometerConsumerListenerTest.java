package org.sdase.commons.server.kafka;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MicrometerConsumerListenerTest {

  private MicrometerConsumerListener listener;
  private MockConsumer<String, String> consumer;

  @BeforeEach
  void setup() {
    consumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
    listener = new MicrometerConsumerListener(Metrics.globalRegistry);
  }

  @Test
  void checkTopicSuccessful() {

    listener.consumerAdded("test-client", consumer);

    consumer.assign(Arrays.asList(new TopicPartition("test-topic", 0)));
    consumer.addRecord(new ConsumerRecord<>("test-topic", 0, 0, "key", "value"));

    // Mock consumers need to seek manually since they cannot automatically reset offsets
    HashMap<TopicPartition, Long> beginningOffsets = new HashMap<>();
    beginningOffsets.put(new TopicPartition("test-topic", 0), 0L);

    consumer.updateBeginningOffsets(beginningOffsets);
    ConsumerRecords<String, String> poll = consumer.poll(Duration.ofSeconds(1L));

    CompositeMeterRegistry globalRegistry = Metrics.globalRegistry;

    System.out.println();
  }
}
