package org.sdase.commons.server.kafka.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

public class TopicConfig {

   @NotNull
   @JsonProperty
   private String name;

   @NotNull
   @JsonProperty
   private Integer replicationFactor = 1;

   @NotNull
   @JsonProperty
   private Integer partitions = 1;

   @JsonProperty
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
      void setName(String name);

      void setReplicationFactor(Integer replicationFactor);

      void setPartitions(Integer partitions);

      void setConfig(Map<String, String> config);

   }

}
