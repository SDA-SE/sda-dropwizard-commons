package org.sdase.commons.server.kafka.consumer.strategies;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class should hold all {@link Closeable}s that need to be cleaned up after a single Kafka
 * message has been handled. After handling, {@link #close()} must be called. All errors on {@link
 * #close()} are suppressed and logged.
 */
public class MessageHandlerContextCloseable implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(MessageHandlerContextCloseable.class);

  private final List<Closeable> contextCloseables;

  private MessageHandlerContextCloseable(List<Closeable> contextCloseables) {
    this.contextCloseables = contextCloseables;
  }

  public static MessageHandlerContextCloseable of(Closeable... closeables) {
    return new MessageHandlerContextCloseable(Arrays.asList(closeables));
  }

  @Override
  public void close() {
    for (Closeable contextCloseable : contextCloseables) {
      try {
        contextCloseable.close();
      } catch (IOException e) {
        LOG.warn("Failed to close.", e);
      }
    }
  }
}
