package org.sdase.commons.server.kafka.config;

import java.util.HashMap;
import java.util.Map;

public class ProducerConfig {

   private Map<String, String> config = new HashMap<>();

   public Map<String, String> getConfig() {
      return config;
   }

   public ProducerConfig setConfig(Map<String, String> config) {
      this.config = config;
      return this;
   }

   public interface ProducerConfigBuilder {
      ProducerConfigBuilder addConfig(String key, String value);

      ProducerConfig build();
   }

   public static Builder builder() {
      return new Builder();
   }

   public static class Builder implements ProducerConfigBuilder {

      private Map<String, String> config = new HashMap<>();

      @Override
      public ProducerConfigBuilder addConfig(String key, String value) {
         config.put(key, value);
         return this;
      }

      @Override
      public ProducerConfig build() {
         ProducerConfig producerConfig = new ProducerConfig();
         producerConfig.setConfig(config);
         return producerConfig;
      }
   }

}
