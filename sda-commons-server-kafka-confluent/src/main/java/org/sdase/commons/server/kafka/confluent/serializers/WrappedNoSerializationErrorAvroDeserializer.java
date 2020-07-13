package org.sdase.commons.server.kafka.confluent.serializers;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import java.util.Map;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * wrap a @{@link KafkaAvroDeserializer} and avoids a @{@link SerializationException}. This can be
 * used to workaround a Kafka problem and ignore invalid records, key/value have to be checked if
 * null.
 *
 * @deprecated Arvo support will be removed in the next major version
 */
@Deprecated
public class WrappedNoSerializationErrorAvroDeserializer<T> // NOSONAR intended deprecation
    implements Deserializer<T> {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(WrappedNoSerializationErrorAvroDeserializer.class);

  private Class<T> clazz;
  private KafkaAvroDeserializer avroDeserializer;

  public WrappedNoSerializationErrorAvroDeserializer(final Class<T> clazz) {
    this.clazz = clazz;
    avroDeserializer = new KafkaAvroDeserializer();
  }

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    avroDeserializer.configure(configs, isKey);
  }

  @Override
  public void close() {
    avroDeserializer.close();
  }

  @Override
  public T deserialize(String topic, byte[] data) {
    if (data == null || data.length == 0) {
      return null;
    }

    try {
      Object o = avroDeserializer.deserialize(topic, data);
      return (clazz.isInstance(o) ? clazz.cast(o) : null);
    } catch (SerializationException e) {
      LOGGER.error("Unable to deserialize record for topic {} due to exception", topic, e);
    }
    return null;
  }
}
