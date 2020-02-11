package org.sdase.commons.server.kafka.consumer.strategies.deadletter.helper;

import java.util.Map;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple helper that delegates all calls to the wrapped deserializer given in the constructor.
 * Potential {@link SerializationException} exceptions are caught and logged.
 *
 * <p>Result of the serialization is then a {@link DeserializerResult} with the raw value and either
 * the deserialized value or the deserialization exception.
 *
 * <p>This helper can be used to workaround a known Kafka problem, see
 * https://issues.apache.org/jira/browse/KAFKA-4740 and is needed for DeadLetterMessageListener
 * strategy to work accordingly by retrying to deliver the failed message.
 *
 * @param <T> class to deserialize
 */
public class NoSerializationErrorDeserializer<T> implements Deserializer<DeserializerResult<T>> {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(NoSerializationErrorDeserializer.class);

  private final Deserializer<T> wrappedDeserializer;

  public NoSerializationErrorDeserializer(Deserializer<T> deserializer) {
    this.wrappedDeserializer = deserializer;
  }

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    wrappedDeserializer.configure(configs, isKey);
  }

  @Override
  public DeserializerResult<T> deserialize(String topic, byte[] data) {
    try {
      T parsedValue = wrappedDeserializer.deserialize(topic, data);
      return DeserializerResult.success(parsedValue, data);
    } catch (SerializationException e) {
      LOGGER.error("Unable to deserialize record for topic {} due to exception", topic, e);
      return DeserializerResult.fail(e, data);
    }
  }

  @Override
  public void close() {
    wrappedDeserializer.close();
  }
}
