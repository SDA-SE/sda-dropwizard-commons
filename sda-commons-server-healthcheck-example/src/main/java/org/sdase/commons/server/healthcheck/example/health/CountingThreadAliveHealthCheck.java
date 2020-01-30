package org.sdase.commons.server.healthcheck.example.health;

import com.codahale.metrics.health.HealthCheck;
import org.sdase.commons.server.healthcheck.example.thread.CountingThread;

/** health check that reports unhealthy if the monitored thread is not alive any longer */
public class CountingThreadAliveHealthCheck extends HealthCheck {

  private CountingThread thread;
  private static final String MESSAGE = "Thread %s status %s";

  @Override
  protected Result check() {
    if (thread != null && thread.isAlive()) {
      return Result.healthy(String.format(MESSAGE, thread.getName(), "alive"));
    } else if (thread != null && thread.isInterrupted()) {
      return Result.unhealthy(String.format(MESSAGE, thread.getName(), "dead"));
    } else {
      return Result.unhealthy(String.format(MESSAGE, "not initialized", "unknown"));
    }
  }

  public void setCountingThread(CountingThread thread) {
    this.thread = thread;
  }

  public CountingThread getThread() {
    return thread;
  }
}
