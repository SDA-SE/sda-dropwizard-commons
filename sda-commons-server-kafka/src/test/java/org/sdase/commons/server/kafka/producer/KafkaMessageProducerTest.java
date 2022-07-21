package org.sdase.commons.server.kafka.producer;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sdase.commons.server.kafka.prometheus.ProducerTopicMessageCounter;

@ExtendWith(MockitoExtension.class)
class KafkaMessageProducerTest {

  @Mock KafkaProducer<String, String> mockProducer;

  @Test
  void shouldDelegateFlushCallToProducer() {
    // given
    KafkaMessageProducer<String, String> kafkaMessageProducer =
        new KafkaMessageProducer<>(
            "topicName", mockProducer, new ProducerTopicMessageCounter(), "producerName");
    // when
    kafkaMessageProducer.flush();
    // then
    verify(mockProducer, times(1)).flush();
  }
}
