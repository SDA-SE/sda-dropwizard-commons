package org.sdase.commons.server.kafka.config;

/** Configuration for Kafka Health Checks. */
public class HealthCheckConfig {

  private int timeoutInSeconds = 2;

  public int getTimeoutInSeconds() {
    return timeoutInSeconds;
  }

  public HealthCheckConfig setTimeoutInSeconds(int timeoutInSeconds) {
    this.timeoutInSeconds = timeoutInSeconds;
    return this;
  }
}
