package org.sdase.commons.server.kafka.topicana;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class TopicanaExceptionsTest {

  @Test
  public void testEvaluationException() {
    IllegalStateException cause = new IllegalStateException("illegal state");
    EvaluationException e = new EvaluationException("msg", cause);
    assertThat(e.getMessage()).isEqualTo("msg");
    assertThat(e.getCause()).isEqualTo(cause);
  }

  @Test
  public void testMismatchedTopicConfigException() {
    Set<String> missingTopics = new HashSet<>();
    missingTopics.add("foo");
    ComparisonResult comparisonResult =
        new ComparisonResult(missingTopics, new HashMap<>(), new HashMap<>(), new HashMap<>());
    MismatchedTopicConfigException e = new MismatchedTopicConfigException(comparisonResult);
    assertThat(e.getResult().getMissingTopics()).isEqualTo(missingTopics);
  }
}
