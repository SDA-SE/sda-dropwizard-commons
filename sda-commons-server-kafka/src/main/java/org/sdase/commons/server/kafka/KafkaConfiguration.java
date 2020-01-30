package org.sdase.commons.server.kafka;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sdase.commons.server.kafka.config.AdminConfig;
import org.sdase.commons.server.kafka.config.ConsumerConfig;
import org.sdase.commons.server.kafka.config.ListenerConfig;
import org.sdase.commons.server.kafka.config.ProducerConfig;
import org.sdase.commons.server.kafka.config.Security;
import org.sdase.commons.server.kafka.config.TopicConfig;

/** Subclass of Dropwizard Configuration class to hold any values necessary to connect to Kafka */
public class KafkaConfiguration {

  private boolean disabled = false;

  private List<String> brokers = new ArrayList<>();

  private Map<String, ProducerConfig> producers = new HashMap<>();

  private Map<String, ConsumerConfig> consumers = new HashMap<>();

  private Map<String, TopicConfig> topics = new HashMap<>();

  private Map<String, ListenerConfig> listenerConfig = new HashMap<>();

  private Security security = new Security();

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
