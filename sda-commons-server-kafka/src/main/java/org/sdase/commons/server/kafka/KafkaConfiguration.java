package org.sdase.commons.server.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.sdase.commons.server.kafka.config.AdminConfig;
import org.sdase.commons.server.kafka.config.ConsumerConfig;
import org.sdase.commons.server.kafka.config.ListenerConfig;
import org.sdase.commons.server.kafka.config.ProducerConfig;
import org.sdase.commons.server.kafka.config.Security;
import org.sdase.commons.server.kafka.config.TopicConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Subclass of Dropwizard Configuration class to hold any values necessary to
 * connect to Kafka
 */
public class KafkaConfiguration {

   @JsonProperty(value = "disabled")
   private boolean disabled = false;

   @JsonProperty(value = "brokers")
   private List<String> brokers = new ArrayList<>();

   @JsonProperty(value = "producers")
   private Map<String, ProducerConfig> producers = new HashMap<>();

   @JsonProperty(value = "consumers")
   private Map<String, ConsumerConfig> consumers = new HashMap<>();

   @JsonProperty(value = "topics")
   private Map<String, TopicConfig> topics = new HashMap<>();

   @JsonProperty(value = "listenerConfig")
   private Map<String, ListenerConfig> listenerConfig = new HashMap<>();

   @JsonProperty(value = "security")
   private Security security = new Security();
   
   @JsonProperty(value = "adminConfig")
   private AdminConfig adminConfig = new AdminConfig();

   public List<String> getBrokers() {
      return brokers;
   }

   public void setBrokers(List<String> brokers) {
      this.brokers = brokers;
   }

   public Security getSecurity() {
      return security;
   }

   public void setSecurity(Security security) {
      this.security = security;
   }

   public Map<String, ProducerConfig> getProducers() {
      return producers;
   }

   public Map<String, ConsumerConfig> getConsumers() {
      return consumers;
   }

   public Map<String, TopicConfig> getTopics() {
      return topics;
   }

   public Map<String, ListenerConfig> getListenerConfig() {
      return listenerConfig;
   }

   public boolean isDisabled() {
      return disabled;
   }

   public void setDisabled(boolean disabled) {
      this.disabled = disabled;
   }

   public void setProducers(Map<String, ProducerConfig> producers) {
      this.producers = producers;
   }

   public void setConsumers(Map<String, ConsumerConfig> consumers) {
      this.consumers = consumers;
   }

   public void setTopics(Map<String, TopicConfig> topics) {
      this.topics = topics;
   }

   public void setListenerConfig(Map<String, ListenerConfig> listenerConfig) {
      this.listenerConfig = listenerConfig;
   }

   public AdminConfig getAdminConfig() {
      return adminConfig;
   }

   public void setAdminConfig(AdminConfig adminConfig) {
      this.adminConfig = adminConfig;
   }
}
