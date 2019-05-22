package org.sdase.commons.server.kafka.config;

import java.util.HashMap;
import java.util.Map;

public class ProducerConfig {

   /**
    * Convenience for setting Kafka's property {@code 'client.id'}. Will not work if you manually
    * add that property to {@link #config}.
    */
   private String clientId;

   private Map<String, String> config = new HashMap<>();

   public Map<String, String> getConfig() {
      if (clientId != null) {
         config.putIfAbsent("client.id", clientId);
      }
      return config;
   }

   public ProducerConfig setConfig(Map<String, String> config) {
      this.config = config;
      return this;
   }

   public String getClientId() {
      return clientId;
   }

   public ProducerConfig setClientId(String clientId) {
      this.clientId = clientId;
      return this;
   }

   public interface ProducerConfigBuilder {
      ProducerConfigBuilder addConfig(String key, String value);
      ProducerConfigBuilder withClientId(String clientId);

      ProducerConfig build();
   }

   public static Builder builder() {
      return new Builder();
   }

   public static class Builder implements ProducerConfigBuilder {

      private String clientId;
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
         producerConfig.setClientId(clientId);
         return producerConfig;
      }

      @Override
      public ProducerConfigBuilder withClientId(String clientId) {
         this.clientId = clientId;
         return this;
      }
   }

}
