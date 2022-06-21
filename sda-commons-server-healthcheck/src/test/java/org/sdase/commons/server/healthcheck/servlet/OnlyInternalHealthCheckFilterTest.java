package org.sdase.commons.server.healthcheck.servlet;

import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.health.HealthCheck;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.healthcheck.ExternalHealthCheck;

class OnlyInternalHealthCheckFilterTest {

  private final OnlyInternalHealthCheckFilter filter = new OnlyInternalHealthCheckFilter();

  @Test
  void ignoreNull() {
    assertThat(filter.matches("null", null)).isFalse();
  }

  @Test
  void identifyAnnotatedCheckAsExternal() {
    assertThat(filter.matches("external", new ExternalBaseHealthCheck())).isFalse();
  }

  @Test
  void identifySubclassOfAnnotatedCheckAsExternal() {
    assertThat(filter.matches("childOfExternal", new ExternalCustomHealthCheck())).isFalse();
  }

  @Test
  void identifyDefaultHealthCheckAsInternal() {
    assertThat(filter.matches("defaultIsInternal", new InternalHealthCheck())).isTrue();
  }

  //
  // health check dummies for testing
  //

  @ExternalHealthCheck
  static class ExternalBaseHealthCheck extends HealthCheck {

    @Override
    protected Result check() {
      return null;
    }
  }

  private static class ExternalCustomHealthCheck extends ExternalBaseHealthCheck {}

  static class InternalHealthCheck extends HealthCheck {

    @Override
    protected Result check() {
      return null;
    }
  }
}
