package org.sdase.commons.server.kafka.consumer.strategies.deadletter;

import com.github.ftrossbach.club_topicana.core.ExpectedTopicConfiguration;

public class TopicConfigurationHolder {

  private final ExpectedTopicConfiguration main;
  private final ExpectedTopicConfiguration retry;
  private final ExpectedTopicConfiguration deadLetter;

  public static TopicConfigurationHolder create(ExpectedTopicConfiguration main) {
    return new TopicConfigurationHolder(main);
  }

  public static TopicConfigurationHolder create(
      ExpectedTopicConfiguration main,
      ExpectedTopicConfiguration retry,
      ExpectedTopicConfiguration deadLetter) {
    return new TopicConfigurationHolder(main, retry, deadLetter);
  }

  /**
   * Creates a new {@link TopicConfigurationHolder} with the given configurations
   *
   * @param main Topic Configuration for the main topic
   * @param retry Topic Configuration for the retry topic
   * @param deadLetter Topic Configuration for the dead letter topic
   */
  private TopicConfigurationHolder(
      ExpectedTopicConfiguration main,
      ExpectedTopicConfiguration retry,
      ExpectedTopicConfiguration deadLetter) {
    this.main = main;
    this.retry = retry;
    this.deadLetter = deadLetter;
  }

  /**
   * Creates a new {@link TopicConfigurationHolder}. The configuration for retry and dead letter
   * topic are deducted from the main topic configiration with respect to partition and replica
   * count
   *
   * @param main Topic Configuration for the main topic
   */
  private TopicConfigurationHolder(ExpectedTopicConfiguration main) {
    this.main = main;
    this.retry = createDefaultTopicConfiguration(main, TopicType.RETRY);
    this.deadLetter = createDefaultTopicConfiguration(main, TopicType.DEAD_LETTER);
  }

  /**
   * Returns or calculate the {@link ExpectedTopicConfiguration} for one of the topics in the dead
   * letter strategy, given by convention.
   *
   * <p>If a topic configuration in the {@link org.sdase.commons.server.kafka.KafkaConfiguration}
   * exists this config is returned. Otherwise, a default configuration is created. Partition and
   * replication count is taken from the main topic configuration.
   *
   * @param topicType defines the kind of topic
   * @return TopicConfiguration for the topic type.
   */
  private ExpectedTopicConfiguration createDefaultTopicConfiguration(
      ExpectedTopicConfiguration main, TopicType topicType) {
    return new ExpectedTopicConfiguration.ExpectedTopicConfigurationBuilder(
            createTopicName(topicType, main))
        .withPartitionCount(main.getPartitions().count())
        .withReplicationFactor(main.getReplicationFactor().count())
        .build();
  }

  /**
   * return the topic name for a dead letter relevant topic
   *
   * @param type type of the topic in context of dead letter
   * @return the topic name as string
   */
  public String getTopicName(TopicType type) {
    switch (type) {
      case RETRY:
        return retry.getTopicName();
      case DEAD_LETTER:
        return deadLetter.getTopicName();
      default:
        return main.getTopicName();
    }
  }

  /**
   * return the topic configuration for a dead letter relevant topic
   *
   * @param type type of the topic in context of dead letter
   * @return the topic configuration
   */
  public ExpectedTopicConfiguration getTopicConfiguration(TopicType type) {
    switch (type) {
      case RETRY:
        return retry;
      case DEAD_LETTER:
        return deadLetter;
      default:
        return main;
    }
  }

  /**
   * Convention for topic naming: retry and dead letter topic must end with suffix
   * '<b>.&lt;topicType&gt;</b>.
   *
   * @param topicType type of the topic for that the name is calculated
   * @return name of the topic defined by the topic type
   */
  private String createTopicName(TopicType topicType, ExpectedTopicConfiguration main) {
    if (topicType == TopicType.MAIN) {
      return main.getTopicName();
    }
    return main.getTopicName() + "." + topicType.toString();
  }

  public enum TopicType {
    DEAD_LETTER("deadLetter"),
    RETRY("retry"),
    MAIN("");

    private String value;

    TopicType(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }
  }
}
