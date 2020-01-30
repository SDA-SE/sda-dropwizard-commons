package org.sdase.commons.server.kafka.confluent.builder;

import java.util.Map;
import org.apache.kafka.common.serialization.Deserializer;
import org.sdase.commons.server.kafka.confluent.serializers.WrappedNoSerializationErrorAvroDeserializer;

/**
 * Helper to create a @{@link WrappedNoSerializationErrorAvroDeserializer} specific for Avro
 * deserializer
 */
public class WrappedAvroDeserializer {

  public interface ClassTypeBuilder<T> {
    ConfigBuilder<T> withClassType(Class<T> clazz);
  }

  public interface ConfigBuilder<T> {
    FinalBuilder<T> withConfigProperties(Map<String, String> configMap);
  }

  public interface FinalBuilder<T> {
    Deserializer<T> build(boolean isKey);
  }

  public static <T> ClassTypeBuilder<T> builder() {
    return new Builder<>();
  }

  private static class Builder<T>
      implements ClassTypeBuilder<T>, ConfigBuilder<T>, FinalBuilder<T> {
    private Class<T> clazz;
    private Map<String, String> configMap;

    private Builder() {}

    @Override
    public ConfigBuilder<T> withClassType(Class<T> clazz) {
      this.clazz = clazz;

      return this;
    }

    @Override
    public FinalBuilder<T> withConfigProperties(Map<String, String> configMap) {
      this.configMap = configMap;

      return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Deserializer<T> build(boolean isKey) {
      Deserializer<T> build = new WrappedNoSerializationErrorAvroDeserializer<>(clazz);
      build.configure(configMap, isKey);

      return build;
    }
  }
}
