package org.sdase.commons.server.kafka.producer;

import static org.mockito.Mockito.*;

import java.util.concurrent.Future;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;
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
            "topicName", mockProducer, mock(ProducerTopicMessageCounter.class), "producerName");
    // when
    kafkaMessageProducer.flush();
    // then
    verify(mockProducer, times(1)).flush();
  }

  @Test
  void callingFlushShouldNotThrowExceptionAndMakeSonarHappy() {
    MessageProducer messageProducer =
        new MessageProducer() {
          @Override
          public Future<RecordMetadata> send(Object key, Object value) {
            return null;
          }

          @Override
          public Future<RecordMetadata> send(Object key, Object value, Headers headers) {
            return null;
          }
        };
    messageProducer.flush();
  }
}
