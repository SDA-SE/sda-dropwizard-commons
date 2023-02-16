package org.sdase.commons.server.kafka.consumer.strategies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MessageHandlerContextCloseableTest {
  @Test
  void shouldCloseAllWithoutException() {

    AtomicInteger counter = new AtomicInteger();

    var messageHandlerContextCloseable =
        MessageHandlerContextCloseable.of(
            counter::incrementAndGet,
            () -> {
              counter.incrementAndGet();
              throw new IOException();
            },
            counter::incrementAndGet);

    assertThatNoException().isThrownBy(messageHandlerContextCloseable::close);
    assertThat(counter).hasValue(3);
  }
}
