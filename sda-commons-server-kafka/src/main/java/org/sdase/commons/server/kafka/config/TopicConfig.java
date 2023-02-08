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

  /**
   * @deprecated since the topic creation will be removed, this method will not be necessary
   *     anymore, and it will be removed as well
   */
  @Deprecated
  public Integer getReplicationFactor() {
    return replicationFactor;
  }

  /**
   * @deprecated since the topic creation will be removed, this method will not be necessary
   *     anymore, and it will be removed as well
   */
  @Deprecated
  public Integer getPartitions() {
    return partitions;
  }

  /**
   * @deprecated since the topic creation will be removed, this method will not be necessary
   *     anymore, and it will be removed as well
   */
  @Deprecated
  public Map<String, String> getConfig() {
    return config;
  }

  public TopicConfig setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * @deprecated since the topic creation will be removed, this method will not be necessary
   *     anymore, and it will be removed as well
   * @param replicationFactor
   */
  @Deprecated
  public void setReplicationFactor(Integer replicationFactor) {
    this.replicationFactor = replicationFactor;
  }

  /**
   * @deprecated since the topic creation will be removed, this method will not be necessary
   *     anymore, and it will be removed as well
   */
  @Deprecated
  public void setPartitions(Integer partitions) {
    this.partitions = partitions;
  }

  /**
   * @deprecated since the topic creation will be removed, this method will not be necessary
   *     anymore, and it will be removed as well
   * @param config
   */
  @Deprecated
  public void setConfig(Map<String, String> config) {
    this.config = config;
  }

  public interface TopicNameBuilder {
    TopicConfigBuilder name(String name);
  }

  public interface TopicConfigBuilder {

    /**
     * @deprecated since the topic creation will be removed, this method will not be necessary
     *     anymore, and it will be removed as well
     * @param replicationFactor
     */
    @Deprecated
    TopicConfigBuilder withReplicationFactor(Integer replicationFactor);

    /**
     * @deprecated since the topic creation will be removed, this method will not be necessary
     *     anymore, and it will be removed as well
     * @param partitions
     */
    @Deprecated
    TopicConfigBuilder withPartitions(Integer partitions);

    /**
     * @deprecated since the topic creation will be removed, this method will not be necessary
     *     anymore, and it will be removed as well
     * @param key
     * @param value
     */
    @Deprecated
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

    /**
     * @deprecated since the topic creation will be removed, this method will not be necessary
     *     anymore, and it will be removed as well
     * @param replicationFactor
     */
    @Deprecated
    @Override
    public TopicConfigBuilder withReplicationFactor(Integer replicationFactor) {
      this.replicationFactor = replicationFactor;
      return this;
    }

    /**
     * @deprecated since the topic creation will be removed, this method will not be necessary
     *     anymore, and it will be removed as well
     * @param partitions
     */
    @Deprecated
    @Override
    public TopicConfigBuilder withPartitions(Integer partitions) {
      this.partitions = partitions;
      return this;
    }

    /**
     * @deprecated since the topic creation will be removed, this method will not be necessary
     *     anymore, and it will be removed as well
     * @param key
     * @param value
     */
    @Deprecated
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
