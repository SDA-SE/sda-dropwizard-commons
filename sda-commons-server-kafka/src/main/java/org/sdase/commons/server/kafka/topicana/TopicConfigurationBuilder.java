package org.sdase.commons.server.kafka.topicana;

/**
 * @deprecated All classes from this package will be removed in the next version because topic
 *     comparison is not recommended.
 */
@Deprecated
public class TopicConfigurationBuilder {

  private TopicConfigurationBuilder() {
    // private constructor to hide the explicit one
  }

  public static ExpectedTopicConfiguration.ExpectedTopicConfigurationBuilder builder(String topic) {
    return new ExpectedTopicConfiguration.ExpectedTopicConfigurationBuilder(topic);
  }
}
