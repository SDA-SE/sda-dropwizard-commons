package org.sdase.commons.server.kafka.consumer.strategies.deadletter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sdase.commons.server.kafka.consumer.strategies.deadletter.TopicConfigurationHolder.TopicType.*;

import com.github.ftrossbach.club_topicana.core.ExpectedTopicConfiguration;
import org.junit.Test;

public class TopicConfigurationHolderTest {

  @Test
  public void shouldGiveTopicDescriptionsFromMainTopic() {
    ExpectedTopicConfiguration main =
        new ExpectedTopicConfiguration.ExpectedTopicConfigurationBuilder("main")
            .withPartitionCount(2)
            .withReplicationFactor(3)
            .build();

    TopicConfigurationHolder given = TopicConfigurationHolder.create(main);

    assertThat(given.getTopicConfiguration(MAIN)).usingRecursiveComparison().isEqualTo(main);
    assertThat(given.getTopicConfiguration(TopicConfigurationHolder.TopicType.RETRY))
        .usingRecursiveComparison()
        .isEqualTo(
            new ExpectedTopicConfiguration.ExpectedTopicConfigurationBuilder("main.retry")
                .withPartitionCount(2)
                .withReplicationFactor(3)
                .build());
    assertThat(given.getTopicConfiguration(TopicConfigurationHolder.TopicType.DEAD_LETTER))
        .usingRecursiveComparison()
        .isEqualTo(
            new ExpectedTopicConfiguration.ExpectedTopicConfigurationBuilder("main.deadLetter")
                .withPartitionCount(2)
                .withReplicationFactor(3)
                .build());

    assertThat(given.getTopicName(MAIN)).isEqualTo("main");
    assertThat(given.getTopicName(RETRY)).isEqualTo("main.retry");
    assertThat(given.getTopicName(DEAD_LETTER)).isEqualTo("main.deadLetter");
  }

  @Test
  public void shouldGiveTopicDescriptionsFromGivenTopics() {
    ExpectedTopicConfiguration main =
        new ExpectedTopicConfiguration.ExpectedTopicConfigurationBuilder("main")
            .withPartitionCount(2)
            .withReplicationFactor(3)
            .build();

    ExpectedTopicConfiguration retry =
        new ExpectedTopicConfiguration.ExpectedTopicConfigurationBuilder("retryTest")
            .withPartitionCount(1)
            .withReplicationFactor(4)
            .build();

    ExpectedTopicConfiguration deadKLetter =
        new ExpectedTopicConfiguration.ExpectedTopicConfigurationBuilder("deadLetterTest")
            .withPartitionCount(1)
            .withReplicationFactor(1)
            .build();

    TopicConfigurationHolder given = TopicConfigurationHolder.create(main, retry, deadKLetter);

    assertThat(given.getTopicConfiguration(MAIN)).usingRecursiveComparison().isEqualTo(main);
    assertThat(given.getTopicConfiguration(TopicConfigurationHolder.TopicType.RETRY))
        .usingRecursiveComparison()
        .isEqualTo(
            new ExpectedTopicConfiguration.ExpectedTopicConfigurationBuilder("retryTest")
                .withPartitionCount(1)
                .withReplicationFactor(4)
                .build());
    assertThat(given.getTopicConfiguration(TopicConfigurationHolder.TopicType.DEAD_LETTER))
        .usingRecursiveComparison()
        .isEqualTo(
            new ExpectedTopicConfiguration.ExpectedTopicConfigurationBuilder("deadLetterTest")
                .withPartitionCount(1)
                .withReplicationFactor(1)
                .build());

    assertThat(given.getTopicName(MAIN)).isEqualTo("main");
    assertThat(given.getTopicName(RETRY)).isEqualTo("retryTest");
    assertThat(given.getTopicName(DEAD_LETTER)).isEqualTo("deadLetterTest");
  }
}
