package org.sdase.commons.server.kafka.config;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

public class ConsumerConfig {

   /**
    * Convenience for setting Kafka's property {@code 'group.id'}. Will not work if you manually
    * add that property to {@link #config}.
    */
   @NotNull
   private String group = "default";

   /**
    * Convenience for setting Kafka's property {@code 'client.id'}. Will not work if you manually
    * add that property to {@link #config}.
    */
   private String clientId;

   private Map<String, String> config = new HashMap<>();

   public Map<String, String> getConfig() {
      if (group != null) {
         config.putIfAbsent("group.id", group);
      }
      if (clientId != null) {
         config.putIfAbsent("client.id", clientId);
      }
      return config;
   }

   public String getGroup() {
      return group;
   }

   public void setGroup(String group) {
      this.group = group;
   }

   public String getClientId() {
      return clientId;
   }

   public ConsumerConfig setClientId(String clientId) {
      this.clientId = clientId;
      return this;
   }

   public void setConfig(Map<String, String> config) {
      this.config = config;
   }

   public interface GroupBuilder extends ConsumerConfigBuilder {
      ConsumerConfigBuilder withGroup(String group);
   }

   public interface ConsumerConfigBuilder {
      ConsumerConfigBuilder addConfig(String key, String value);

      ConsumerConfigBuilder withClientId(String clientId);
      ConsumerConfig build();
   }

   public static GroupBuilder builder() {
      return new Builder();
   }

   public static class Builder implements ConsumerConfigBuilder, GroupBuilder {

      private String group = "default";
      private String clientId;
      private Map<String, String> config = new HashMap<>();

      @Override
      public ConsumerConfigBuilder addConfig(String key, String value) {
         config.put(key, value);
         return this;
      }

      @Override
      public ConsumerConfig build() {
         ConsumerConfig consumerConfig = new ConsumerConfig();
         consumerConfig.setConfig(config);
         consumerConfig.setGroup(group);
         consumerConfig.setClientId(clientId);
         return consumerConfig;
      }

      @Override
      public ConsumerConfigBuilder withGroup(String group) {
         this.group = group;
         return this;
      }

      @Override
      public ConsumerConfigBuilder withClientId(String clientId) {
         this.clientId = clientId;
         return this;
      }
   }

}
