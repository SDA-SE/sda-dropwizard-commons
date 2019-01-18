package org.sdase.commons.server.healthcheck.bundle;

import com.codahale.metrics.health.HealthCheck;
import org.junit.Test;
import org.sdase.commons.server.healthcheck.ExternalHealthCheck;

import static org.assertj.core.api.Assertions.assertThat;

public class OnlyInternalHealthCheckFilterTest {

   private OnlyInternalHealthCheckFilter filter = new OnlyInternalHealthCheckFilter();

   @Test
   public void ignoreNull() {
      assertThat(filter.matches("null", null)).isFalse();
   }

   @Test
   public void identifyAnnotatedCheckAsExternal() {
      assertThat(filter.matches("external", new ExternalBaseHealthCheck())).isFalse();
   }

   @Test
   public void identifySubclassOfAnnotatedCheckAsExternal() {
      assertThat(filter.matches("childOfExternal", new ExternalCustomHealthCheck())).isFalse();
   }

   @Test
   public void identifyDefaultHealthCheckAsInternal() {
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

   private static class ExternalCustomHealthCheck extends ExternalBaseHealthCheck {

   }

   static class InternalHealthCheck extends HealthCheck {

      @Override
      protected Result check() {
         return null;
      }
   }

}