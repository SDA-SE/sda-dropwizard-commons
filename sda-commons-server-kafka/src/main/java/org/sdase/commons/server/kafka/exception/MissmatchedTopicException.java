package org.sdase.commons.server.kafka.exception;

import com.github.ftrossbach.club_topicana.core.ComparisonResult;

public class MissmatchedTopicException extends RuntimeException {

  private final transient ComparisonResult comparisonResult;

  public MissmatchedTopicException(ComparisonResult comparisonResult) {
    super(comparisonResult.toString());
    this.comparisonResult = comparisonResult;
  }

  public ComparisonResult getComparisonResult() {
    return comparisonResult;
  }
}
