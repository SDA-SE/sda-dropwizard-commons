package org.sdase.commons.server.kafka.serializers;

import java.util.Map;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple helper that delegates all deserialization calls to the wrapped deserializer given in the
 * constructor. Potential deserialization exceptions are caught, logged and null value is returned.
 * This helper can be used to workaround a known Kafka problem, see
 * https://issues.apache.org/jira/browse/KAFKA-4740.
 *
 * @param <T> class to deserialize
 */
public class WrappedNoSerializationErrorDeserializer<T> implements Deserializer<T> {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(WrappedNoSerializationErrorDeserializer.class);

  private Deserializer<T> wrappedDeserializer;

  public WrappedNoSerializationErrorDeserializer(Deserializer<T> deserializer) {
    this.wrappedDeserializer = deserializer;
  }

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    wrappedDeserializer.configure(configs, isKey);
  }

  @Override
  public T deserialize(String topic, byte[] data) {
    try {
      return wrappedDeserializer.deserialize(topic, data);
    } catch (SerializationException e) {
      LOGGER.error("Unable to deserialize record for topic {} due to exception", topic, e);
    }
    return null;
  }

  @Override
  public void close() {
    wrappedDeserializer.close();
  }
}
