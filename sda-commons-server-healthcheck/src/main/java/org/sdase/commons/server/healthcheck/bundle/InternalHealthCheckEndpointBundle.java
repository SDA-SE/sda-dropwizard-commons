package org.sdase.commons.server.healthcheck.bundle;

import io.dropwizard.Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class InternalHealthCheckEndpointBundle implements Bundle {

   private InternalHealthCheckEndpointBundle() {
      // deny public access
   }

   @Override
   public void run(Environment environment) {
      // Register a new endpoints that provides only the internal helth checks
      // The default healthcheck endpoint '/healthcheck' provides both, internal and external helthchecks
      environment
            .admin()
            .addServlet("Internal Health Check", new OnlyInternalHealthCheckServlet(environment.healthChecks()))
            .addMapping("/healthcheck/internal");
   }

   @Override
   public void initialize(Bootstrap<?> bootstrap) {
      // Nothing here
   }

   public static Builder builder() {
      return new Builder();
   }

   public interface InternalHealthCheckEndpointBuilder {
      InternalHealthCheckEndpointBundle build();
   }

   public static class Builder implements InternalHealthCheckEndpointBuilder {

      private Builder() {
         // deny public access
      }

      @Override
      public InternalHealthCheckEndpointBundle build() {
         return new InternalHealthCheckEndpointBundle();
      }

   }

}