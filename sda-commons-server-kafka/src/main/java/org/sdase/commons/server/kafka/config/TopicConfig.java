package org.sdase.commons.server.kafka.config;

import javax.validation.constraints.NotNull;

public class TopicConfig {

  @NotNull private String name;

  public String getName() {
    return name;
  }

  public TopicConfig setName(String name) {
    this.name = name;
    return this;
  }

  public interface TopicNameBuilder {
    TopicConfigBuilder name(String name);
  }

  public interface TopicConfigBuilder {
    TopicConfig build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder implements TopicConfigBuilder, TopicNameBuilder {

    private String name;

    @Override
    public TopicConfigBuilder name(String name) {
      this.name = name;
      return this;
    }

    @Override
    public TopicConfig build() {
      TopicConfig topicConfig = new TopicConfig();
      topicConfig.setName(name);
      return topicConfig;
    }
  }
}
