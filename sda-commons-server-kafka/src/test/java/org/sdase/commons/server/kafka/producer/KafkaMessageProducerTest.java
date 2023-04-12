package org.sdase.commons.server.kafka.producer;

import static org.mockito.Mockito.*;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.header.internals.RecordHeaders;
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
    MessageProducer<Object, Object> messageProducer = (k, v, h, c) -> null;
    messageProducer.flush();
  }

  @Test
  void shouldInvokeCallback() {
    // given
    KafkaMessageProducer<String, String> kafkaMessageProducer =
        new KafkaMessageProducer<>(
            "topicName", mockProducer, mock(ProducerTopicMessageCounter.class), "producerName");

    Callback callback = (metadata, exception) -> {};

    // when
    kafkaMessageProducer.send("key", "value", new RecordHeaders(), callback);

    // then
    verify(mockProducer, times(1)).send(any(), eq(callback));
  }
}
