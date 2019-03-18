package org.sdase.commons.server.kafka.config;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

public class ConsumerConfig {

   @NotNull
   private String group = "default";

   private Map<String, String> config = new HashMap<>();


   public Map<String, String> getConfig() {
      config.put("group.id", group);
      return config;
   }

   public String getGroup() {
      return group;
   }

   public void setGroup(String group) {
      this.group = group;
   }

   public void setConfig(Map<String, String> config) {
      this.config = config;
   }

   public interface GroupBuilder extends ConsumerConfigBuilder {
      ConsumerConfigBuilder withGroup(String group);
   }

   public interface ConsumerConfigBuilder {
      ConsumerConfigBuilder addConfig(String key, String value);
      ConsumerConfig build();
   }

   public static GroupBuilder builder() {
      return new Builder();
   }

   public static class Builder implements ConsumerConfigBuilder, GroupBuilder {

      private String group = "default";
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
         return consumerConfig;
      }

      @Override
      public ConsumerConfigBuilder withGroup(String group) {
         this.group = group;
         return this;
      }
   }

}
