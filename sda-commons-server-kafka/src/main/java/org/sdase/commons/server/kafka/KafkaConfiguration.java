package org.sdase.commons.server.kafka;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sdase.commons.server.kafka.config.AdminConfig;
import org.sdase.commons.server.kafka.config.ConsumerConfig;
import org.sdase.commons.server.kafka.config.HealthCheckConfig;
import org.sdase.commons.server.kafka.config.ListenerConfig;
import org.sdase.commons.server.kafka.config.ProducerConfig;
import org.sdase.commons.server.kafka.config.Security;
import org.sdase.commons.server.kafka.config.TopicConfig;

/** Class to hold any values necessary to connect to Kafka */
public class KafkaConfiguration {

  private boolean disabled = false;

  private List<String> brokers = new ArrayList<>();

  private Map<String, String> config = new HashMap<>();

  private Map<String, ProducerConfig> producers = new HashMap<>();

  private Map<String, ConsumerConfig> consumers = new HashMap<>();

  private Map<String, TopicConfig> topics = new HashMap<>();

  private Map<String, ListenerConfig> listenerConfig = new HashMap<>();

  private Security security = new Security();

  private AdminConfig adminConfig = new AdminConfig();

  private HealthCheckConfig healthCheck = new HealthCheckConfig();

  public List<String> getBrokers() {
    return brokers;
  }

  public KafkaConfiguration setBrokers(List<String> brokers) {
    this.brokers = brokers;
    return this;
  }

  public Map<String, String> getConfig() {
    return config;
  }

  public KafkaConfiguration setConfig(Map<String, String> config) {
    this.config = config;
    return this;
  }

  public Security getSecurity() {
    return security;
  }

  public KafkaConfiguration setSecurity(Security security) {
    this.security = security;
    return this;
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

  public KafkaConfiguration setDisabled(boolean disabled) {
    this.disabled = disabled;
    return this;
  }

  public KafkaConfiguration setProducers(Map<String, ProducerConfig> producers) {
    this.producers = producers;
    return this;
  }

  public KafkaConfiguration setConsumers(Map<String, ConsumerConfig> consumers) {
    this.consumers = consumers;
    return this;
  }

  public KafkaConfiguration setTopics(Map<String, TopicConfig> topics) {
    this.topics = topics;
    return this;
  }

  public KafkaConfiguration setListenerConfig(Map<String, ListenerConfig> listenerConfig) {
    this.listenerConfig = listenerConfig;
    return this;
  }

  public AdminConfig getAdminConfig() {
    return adminConfig;
  }

  public KafkaConfiguration setAdminConfig(AdminConfig adminConfig) {
    this.adminConfig = adminConfig;
    return this;
  }

  public HealthCheckConfig getHealthCheck() {
    return healthCheck;
  }

  public KafkaConfiguration setHealthCheck(HealthCheckConfig healthCheck) {
    this.healthCheck = healthCheck;
    return this;
  }
}
