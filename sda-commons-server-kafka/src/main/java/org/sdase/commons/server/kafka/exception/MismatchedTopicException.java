package org.sdase.commons.server.kafka.exception;

import org.sdase.commons.server.kafka.topicana.ComparisonResult;

/** @deprecated since no comparison will be executed on next version, this class will be removed */
@Deprecated
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
