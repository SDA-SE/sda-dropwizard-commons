package org.sdase.commons.server.kafka.consumer.strategies.deadletter;

public enum TopicType {

  DEAD_LETTER("deadLetter"), RETRY("retry"), MAIN("");

  private String value;

  TopicType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

}
