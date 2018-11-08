package com.sdase.commons.server.prometheus.test;

import com.codahale.metrics.health.HealthCheck;
import com.sdase.commons.server.prometheus.PrometheusBundle;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;

@Path("/")
public class PrometheusTestApplication extends Application<Configuration> {


   @Override
   public void initialize(final Bootstrap<Configuration> bootstrap) {
      bootstrap.addBundle(PrometheusBundle.builder().build());
   }

   @Override
   public void run(Configuration config, final Environment environment) {
      environment.jersey().register(this);

      environment.healthChecks().register("anUnhealthyCheck", new HealthCheck() {
         @Override
         protected Result check() {
            return HealthCheck.Result.unhealthy("always unhealthy");
         }
      });

      environment.healthChecks().register("aHealthyCheck", new HealthCheck() {
         @Override
         protected Result check() {
            return HealthCheck.Result.healthy("always healthy");
         }
      });

      // dummy implementation of the consumer token filter
      environment.jersey().register((ContainerRequestFilter) requestContext
            -> requestContext.setProperty("Consumer-Name", requestContext.getHeaders().getFirst("Consumer-Name")));

   }

   @GET
   @Path("/ping")
   public Response pingResource() {
      return Response.ok("pong").build();
   }

   @GET
   @Path("/path/{param}")
   public Response pathWithSegment(@PathParam("param") String pathParam) {
      return Response.ok(pathParam).build();
   }



}
