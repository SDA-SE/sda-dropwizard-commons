package org.sdase.commons.server.kafka.consumer.strategies.deadletter.helper;

import static java.util.Objects.nonNull;

public final class DeserializerResult<T> {

  private final RuntimeException error;
  private final T parsedValue;
  private final byte[] byteValue;

  private DeserializerResult(RuntimeException error, T parsedValue, byte[] byteValue) {
    this.error = error;
    this.parsedValue = parsedValue;
    this.byteValue = byteValue;
  }

  static <T> DeserializerResult<T> success(T parsedValue, byte[] byteValue) {
    return new DeserializerResult<>(null, parsedValue, byteValue);
  }

  static <T> DeserializerResult<T> fail(RuntimeException e, byte[] byteValue) {
    return new DeserializerResult<>(e, null, byteValue);
  }

  public RuntimeException getError() {
    return this.error;
  }

  public T getParsedValue() {
    return this.parsedValue;
  }

  public boolean hasError() {
    return nonNull(error);
  }

  public boolean parsedSuccessful() {
    return nonNull(parsedValue);
  }

  public byte[] getByteValue() {
    return byteValue;
  }
}
