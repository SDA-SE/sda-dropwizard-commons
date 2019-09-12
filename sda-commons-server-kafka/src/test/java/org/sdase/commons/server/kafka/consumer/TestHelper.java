package org.sdase.commons.server.kafka.consumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;

class TestHelper {

  private TestHelper() {
    // no instance
  }

  static ConsumerRecords<String, String> createConsumerRecords(int noMessages, String ... topics) {
    Map<TopicPartition, List<ConsumerRecord<String, String>>> payload = new HashMap<>();

    for (String topic : topics) {
      TopicPartition tp = new TopicPartition(topic, 0);

      List<ConsumerRecord<String, String>> messages = new ArrayList<>();
      for (int i = 0; i < noMessages; i++) {
        ConsumerRecord<String, String> cr = new ConsumerRecord<>(topic, 0, 0, topic, UUID.randomUUID().toString());

        messages.add(cr);
      }

      payload.put(tp, messages);
    }

    return new ConsumerRecords<>(payload);

  }
}
