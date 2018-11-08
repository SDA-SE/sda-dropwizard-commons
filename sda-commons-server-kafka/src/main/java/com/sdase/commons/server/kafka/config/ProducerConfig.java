package com.sdase.commons.server.kafka.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class ProducerConfig {

   @JsonProperty("config")
   private Map<String, String> config = new HashMap<>();

   public Map<String, String> getConfig() {
      return config;
   }
}
