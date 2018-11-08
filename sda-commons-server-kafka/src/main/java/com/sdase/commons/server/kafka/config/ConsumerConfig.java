package com.sdase.commons.server.kafka.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

public class ConsumerConfig {

   @NotNull
   @JsonProperty(value = "group", defaultValue = "default")
   private String group = "default";

   @JsonProperty("config")
   private Map<String, String> config = new HashMap<>();


   public Map<String, String> getConfig() {
      config.put("group.id", group);
      return config;
   }

}
