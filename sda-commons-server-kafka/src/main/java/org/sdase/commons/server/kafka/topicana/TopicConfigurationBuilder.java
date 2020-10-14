package org.sdase.commons.server.kafka.topicana;

public class TopicConfigurationBuilder {

  private TopicConfigurationBuilder() {
    // private constructor to hide the explicit one
  }

  public static ExpectedTopicConfiguration.ExpectedTopicConfigurationBuilder builder(String topic) {
    return new ExpectedTopicConfiguration.ExpectedTopicConfigurationBuilder(topic);
  }
}
