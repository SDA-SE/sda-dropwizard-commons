package org.sdase.commons.server.healthcheck.example;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.sdase.commons.server.healthcheck.InternalHealthCheckEndpointBundle;
import org.sdase.commons.server.healthcheck.example.health.CountingThreadAliveHealthCheck;
import org.sdase.commons.server.healthcheck.example.thread.CountingThread;
import org.sdase.commons.server.healthcheck.helper.ExternalServiceHealthCheck;

/** Example application for demonstrating how to create and register health checks */
public class HealthExampleApplication extends Application<HealthExampleConfiguration> {

  /** Health check as class variable to influence result for demonstration issues */
  private CountingThreadAliveHealthCheck threadAliveHealthCheck;

  @Override
  public void initialize(final Bootstrap<HealthExampleConfiguration> bootstrap) {
    // creates health check endpoint with internal checks only
    bootstrap.addBundle(InternalHealthCheckEndpointBundle.builder().build());
  }

  @Override
  public void run(final HealthExampleConfiguration config, final Environment environment) {
    // example for registering an external health check
    // Here, the provided ExternalServiceHealthCheck from the sda-commons library is used
    environment
        .healthChecks()
        .register(
            "externalService",
            new ExternalServiceHealthCheck(config.getExternalServiceUrl(), 1000));
    // example for registering a new internal health check
    // Here, it is a class variable for testing reasons. This is not required.
    threadAliveHealthCheck = new CountingThreadAliveHealthCheck();
    environment.healthChecks().register("threadAlive", threadAliveHealthCheck);
  }

  /**
   * method only for testing issues: changes internal state of the application to simulate healthy
   * and unhealthy
   */
  void stopCountingThread() throws InterruptedException {
    CountingThread counting = threadAliveHealthCheck.getThread();
    counting.setStop(true);

    // wait until thread is dead
    counting.join();
  }

  /** method only for testing issues: resets initial state of the application */
  void startCountingThread() {
    CountingThread counting = new CountingThread();
    counting.start();
    threadAliveHealthCheck.setCountingThread(counting);
  }

  public static void main(String[] args) throws Exception {
    new HealthExampleApplication().run(args);
  }
}
