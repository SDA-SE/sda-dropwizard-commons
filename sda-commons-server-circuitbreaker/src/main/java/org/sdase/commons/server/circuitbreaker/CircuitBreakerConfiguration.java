package org.sdase.commons.server.circuitbreaker;

import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_FAILURE_RATE_THRESHOLD;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_MINIMUM_NUMBER_OF_CALLS;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_PERMITTED_CALLS_IN_HALF_OPEN_STATE;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_WAIT_DURATION_IN_OPEN_STATE;

import java.time.Duration;

public class CircuitBreakerConfiguration {

  private float failureRateThreshold = DEFAULT_FAILURE_RATE_THRESHOLD;
  private int ringBufferSizeInHalfOpenState = DEFAULT_PERMITTED_CALLS_IN_HALF_OPEN_STATE;
  private int ringBufferSizeInClosedState = DEFAULT_MINIMUM_NUMBER_OF_CALLS;
  private Duration waitDurationInOpenState =
      Duration.ofSeconds(DEFAULT_WAIT_DURATION_IN_OPEN_STATE);

  public float getFailureRateThreshold() {
    return failureRateThreshold;
  }

  public CircuitBreakerConfiguration setFailureRateThreshold(float failureRateThreshold) {
    this.failureRateThreshold = failureRateThreshold;
    return this;
  }

  public int getRingBufferSizeInHalfOpenState() {
    return ringBufferSizeInHalfOpenState;
  }

  public CircuitBreakerConfiguration setRingBufferSizeInHalfOpenState(
      int ringBufferSizeInHalfOpenState) {
    this.ringBufferSizeInHalfOpenState = ringBufferSizeInHalfOpenState;
    return this;
  }

  public int getRingBufferSizeInClosedState() {
    return ringBufferSizeInClosedState;
  }

  public CircuitBreakerConfiguration setRingBufferSizeInClosedState(
      int ringBufferSizeInClosedState) {
    this.ringBufferSizeInClosedState = ringBufferSizeInClosedState;
    return this;
  }

  public Duration getWaitDurationInOpenState() {
    return waitDurationInOpenState;
  }

  public CircuitBreakerConfiguration setWaitDurationInOpenState(Duration waitDurationInOpenState) {
    this.waitDurationInOpenState = waitDurationInOpenState;
    return this;
  }
}
