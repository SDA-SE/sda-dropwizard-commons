package org.sdase.commons.server.kafka.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;
import org.sdase.commons.server.kafka.config.ListenerConfig;
import org.sdase.commons.server.kafka.consumer.strategies.MessageListenerStrategy;

class MessageListenerErrorOnCloseTest {

  @SuppressWarnings("unchecked")
  KafkaConsumer<String, String> consumer = mock(KafkaConsumer.class);

  @SuppressWarnings("unchecked")
  MessageListenerStrategy<String, String> strategy = mock(MessageListenerStrategy.class);

  MessageListener<String, String> messageListener =
      new MessageListener<>(List.of("test-topic"), consumer, ListenerConfig.getDefault(), strategy);

  @Test
  @StdIo
  void shouldLogErrorOnCommit(StdOut out) throws InterruptedException {
    doThrow(new RuntimeException("Test exception commit")).when(strategy).commitOnClose(any());
    Thread thread = new Thread(messageListener);
    thread.start();
    messageListener.stopConsumer();
    thread.join();
    verify(strategy, times(1)).commitOnClose(any());
    assertThat(String.join("\n", out.capturedLines()))
        .contains("Exception caught while committing offsets on close.")
        .contains("Test exception commit");
  }

  @Test
  @StdIo
  void shouldLogErrorOnClose(StdOut out) throws InterruptedException {
    doThrow(new RuntimeException("Test exception close")).when(consumer).close();
    Thread thread = new Thread(messageListener);
    thread.start();
    messageListener.stopConsumer();
    thread.join();
    verify(strategy, times(1)).commitOnClose(any());
    assertThat(String.join("\n", out.capturedLines()))
        .contains("Exception caught while closing consumer.")
        .contains("Test exception close");
  }
}
