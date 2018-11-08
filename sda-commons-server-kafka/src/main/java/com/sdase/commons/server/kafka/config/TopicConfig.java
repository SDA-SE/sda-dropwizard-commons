package com.sdase.commons.server.kafka.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

public class TopicConfig {

   @NotNull
   @JsonProperty("name")
   private String name;

   @NotNull
   @JsonProperty(value = "replicationFactor", defaultValue = "1")
   private Integer replicationFactor;

   @NotNull
   @JsonProperty(value = "partitions", defaultValue = "1")
   private Integer partitions;

   @JsonProperty("config")
   private Map<String, String> config = new HashMap<>();

   public String getName() {
      return name;
   }

   public Integer getReplicationFactor() {
      return replicationFactor;
   }

   public Integer getPartitions() {
      return partitions;
   }

   public Map<String, String> getConfig() {
      return config;
   }


   public interface TopicConfigBuilder {
      public void setName(String name);

      public void setReplicationFactor(Integer replicationFactor);

      public void setPartitions(Integer partitions);

      public void setConfig(Map<String, String> config);

   }

}
