package org.sdase.commons.server.kafka.exception;

import org.sdase.commons.server.kafka.topicana.ComparisonResult;

public class MismatchedTopicException extends RuntimeException {

  private final transient ComparisonResult comparisonResult;

  public MismatchedTopicException(ComparisonResult comparisonResult) {
    super(comparisonResult.toString());
    this.comparisonResult = comparisonResult;
  }

  public ComparisonResult getComparisonResult() {
    return comparisonResult;
  }
}
