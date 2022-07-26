package org.sdase.commons.server.kafka.config;

import javax.validation.constraints.NotNull;

public class ListenerConfig {

  private int instances = 1;
  private long topicMissingRetryMs = 0;
  private long pollInterval = 100;
  private long pollIntervalFactorOnError = 4;
  private long maxPollInterval = 25_000;

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

  public long getPollIntervalFactorOnError() {
    return pollIntervalFactorOnError;
  }

  public void setPollIntervalFactorOnError(long pollIntervalFactorOnError) {
    this.pollIntervalFactorOnError = pollIntervalFactorOnError;
  }

  public long getMaxPollInterval() {
    return maxPollInterval;
  }

  public void setMaxPollInterval(long maxPollInterval) {
    this.maxPollInterval = maxPollInterval;
  }

  public static class ListenerConfigBuilder {

    private long topicMissingRetryMs = 0;
    private long pollInterval = 100;
    private long pollIntervalFactorOnError = 4;
    private long maxPollInterval = 25_000;

    public ListenerConfigBuilder withTopicMissingRetryMs(@NotNull long ms) {
      this.topicMissingRetryMs = ms;
      return this;
    }

    public ListenerConfigBuilder withPollInterval(@NotNull long ms) {
      this.pollInterval = ms;
      return this;
    }

    public ListenerConfigBuilder withMaxPollInterval(@NotNull long ms) {
      this.maxPollInterval = ms;
      return this;
    }

    public ListenerConfigBuilder withPollIntervalFactorOnError(@NotNull long factor) {
      this.pollIntervalFactorOnError = factor;
      return this;
    }

    public ListenerConfig build(@NotNull int numberInstances) {
      ListenerConfig build = new ListenerConfig();
      build.setTopicMissingRetryMs(topicMissingRetryMs);
      build.setPollInterval(pollInterval);
      build.setMaxPollInterval(maxPollInterval);
      build.setPollIntervalFactorOnError(pollIntervalFactorOnError);
      build.setInstances(numberInstances);
      return build;
    }
  }
}
