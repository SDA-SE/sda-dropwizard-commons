package org.sdase.commons.server.circuitbreaker.builder;

import io.dropwizard.Configuration;

public interface CircuitBreakerExceptionBuilder<T extends Configuration>
    extends CircuitBreakerFinalBuilder {

  /**
   * Configures a list of error classes that are recorded as a failure and thus increase the failure
   * rate. Any exception matching or inheriting from one of the list should count as a failure,
   * unless ignored
   *
   * @param errorClasses the error classes that are recorded
   * @return the same builder instance
   */
  CircuitBreakerExceptionBuilder<T> recordExceptions(Class<? extends Throwable>... errorClasses);

  /**
   * Configures a list of error classes that are ignored as a failure and thus do not increase the
   * failure rate. Any exception matching or inheriting from one of the list will not count as a
   * failure, even if marked via record.
   *
   * @param errorClasses the error classes that are ignored
   * @return the same builder instance
   */
  CircuitBreakerExceptionBuilder<T> ignoreExceptions(Class<? extends Throwable>... errorClasses);
}
