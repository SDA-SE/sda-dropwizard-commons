package org.sdase.commons.server.kafka.config;

import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;

public class TopicConfig {

  @NotNull private String name;

  @NotNull private Integer replicationFactor = 1;

  @NotNull private Integer partitions = 1;

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

  public TopicConfig setName(String name) {
    this.name = name;
    return this;
  }

  public void setReplicationFactor(Integer replicationFactor) {
    this.replicationFactor = replicationFactor;
  }

  public void setPartitions(Integer partitions) {
    this.partitions = partitions;
  }

  public void setConfig(Map<String, String> config) {
    this.config = config;
  }

  public interface TopicNameBuilder {
    TopicConfigBuilder name(String name);
  }

  public interface TopicConfigBuilder {

    TopicConfigBuilder withReplicationFactor(Integer replicationFactor);

    TopicConfigBuilder withPartitions(Integer partitions);

    TopicConfigBuilder addConfig(String key, String value);

    TopicConfig build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder implements TopicConfigBuilder, TopicNameBuilder {

    private String name;
    private Integer partitions = 1;
    private Integer replicationFactor = 1;
    private Map<String, String> config = new HashMap<>();

    @Override
    public TopicConfigBuilder name(String name) {
      this.name = name;
      return this;
    }

    @Override
    public TopicConfigBuilder withReplicationFactor(Integer replicationFactor) {
      this.replicationFactor = replicationFactor;
      return this;
    }

    @Override
    public TopicConfigBuilder withPartitions(Integer partitions) {
      this.partitions = partitions;
      return this;
    }

    @Override
    public TopicConfigBuilder addConfig(String key, String value) {
      config.put(key, value);
      return this;
    }

    @Override
    public TopicConfig build() {
      TopicConfig topicConfig = new TopicConfig();
      topicConfig.setName(name);
      topicConfig.setConfig(config);
      topicConfig.setPartitions(partitions);
      topicConfig.setReplicationFactor(replicationFactor);
      return topicConfig;
    }
  }
}
