package org.sdase.commons.server.kafka.config;

import javax.validation.constraints.NotNull;

public class ListenerConfig {

  private int instances = 1;
  private long topicMissingRetryMs = 0;
  private long pollInterval = 100;

  private ListenerConfig() {
    // empty constructor for jackson
  }

  public static ListenerConfig getDefault() {
    return new ListenerConfig();
  }

  public static ListenerConfigBuilder builder() {
    return new ListenerConfigBuilder();
  }

  public int getInstances() {
    return instances;
  }

  public void setInstances(int instances) {
    this.instances = instances;
  }

  public long getTopicMissingRetryMs() {
    return topicMissingRetryMs;
  }

  public void setTopicMissingRetryMs(long topicMissingRetryMs) {
    this.topicMissingRetryMs = topicMissingRetryMs;
  }

  public long getPollInterval() {
    return pollInterval;
  }

  public void setPollInterval(long pollInterval) {
    this.pollInterval = pollInterval;
  }

  public static class ListenerConfigBuilder {

    private long topicMissingRetryMs = 0;
    private long pollInterval = 100;

    public ListenerConfigBuilder withTopicMissingRetryMs(@NotNull long ms) {
      this.topicMissingRetryMs = ms;
      return this;
    }

    public ListenerConfigBuilder withPollInterval(@NotNull long ms) {
      this.pollInterval = ms;
      return this;
    }

    public ListenerConfig build(@NotNull int numberInstances) {
      ListenerConfig build = new ListenerConfig();
      build.topicMissingRetryMs = topicMissingRetryMs;
      build.pollInterval = pollInterval;
      build.instances = numberInstances;
      return build;
    }
  }
}
