package org.sdase.commons.server.prometheus.example;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.core.metrics.Histogram;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A service that produces metrics about important things. */
public class MyServiceWithMetrics {

  private static final Logger LOG = LoggerFactory.getLogger(MyServiceWithMetrics.class);

  // Create a Histogram to track execution times. Note that histogram metric names should use the
  // unit of the
  // recorded value as suffix. Usually durations are tracked in seconds. Fractional seconds can be
  // tracked as
  // double value.
  private Histogram someOperationDurationTracker =
      Histogram.builder()
          .name("some_operation_execution_duration_seconds")
          .help("Tracks duration of some operation.")
          .register();

  // Create a Counter to count events.
  private Counter someOperationSuccessCounter =
      Counter.builder()
          .name("some_operation_success_counter_total")
          .help("Counts successes occurred when some operation is invoked.")
          .register();

  // Create a Counter to count events.
  private Counter someOperationErrorCounter =
      Counter.builder()
          .name("some_operation_error_counter_total")
          .help("Counts errors occurred when some operation is invoked.")
          .register();

  // Create a Gauge to track the most recent value of a metric. The name of a gauge metric should
  // use the unit of the
  // recorded value as suffix.
  private Gauge someOperationCurrentValueGauge =
      Gauge.builder()
          .name("some_operation_temperature_celsius")
          .help("Tracks the temperature recorded within the operation.")
          .register();

  private Random random = new Random();

  public void doSomeOperationWithTrackedDuration() {

    someOperationDurationTracker.time(
        () -> {
          // here some business logic is invoked which execution time will be tracked
          try {
            Thread.sleep(random.nextInt(2000) + 500L);
          } catch (InterruptedException e) {
            LOG.warn("Interrupted!", e);
            Thread.currentThread().interrupt();
          }
        });
  }

  public void doSomeOperationWithCounting() {

    // do some business logic here and realize if it is successful or not
    boolean success = random.nextBoolean();

    // track the success by incrementing the counter
    if (success) {
      someOperationSuccessCounter.inc();
    } else {
      someOperationErrorCounter.inc();
    }
  }

  public void doSomeOperationWithGauge() {

    // do some business logic that exposes the current value of some metric
    double temperature = random.nextDouble() * 40 - 10; // random temperature from -10°C to 30°C

    someOperationCurrentValueGauge.set(temperature);
  }
}
