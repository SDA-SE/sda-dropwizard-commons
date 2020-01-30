package org.sdase.commons.server.kafka.consumer.strategies.retryprocessingerror;

public class ProcessingErrorRetryException extends RuntimeException {

  public ProcessingErrorRetryException(String message) {
    super(message);
  }

  public ProcessingErrorRetryException(String message, Throwable cause) {
    super(message, cause);
  }

  public ProcessingErrorRetryException(Throwable cause) {
    super(cause);
  }

  public ProcessingErrorRetryException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
