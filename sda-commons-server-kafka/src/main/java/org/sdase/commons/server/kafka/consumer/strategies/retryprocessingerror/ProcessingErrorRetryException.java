package org.sdase.commons.server.kafka.consumer.strategies.retryprocessingerror;

public class ProcessingErrorRetryException extends RuntimeException {
  public ProcessingErrorRetryException(String message) {
    super(message);
  }
}
