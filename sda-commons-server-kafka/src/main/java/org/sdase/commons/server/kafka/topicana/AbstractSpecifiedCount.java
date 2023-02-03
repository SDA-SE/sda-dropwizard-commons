package org.sdase.commons.server.kafka.topicana;

/**
 * @deprecated All classes from this package will be removed in the next version because topic
 *     comparison is not recommended.
 */
@Deprecated
public abstract class AbstractSpecifiedCount {

  private final int count;

  AbstractSpecifiedCount(int count) {
    if (count <= 0) {
      throw new IllegalArgumentException("Count must be larger than 0");
    }
    this.count = count;
  }

  public boolean isSpecified() {
    return true;
  }

  public int count() {
    return count;
  }
}
