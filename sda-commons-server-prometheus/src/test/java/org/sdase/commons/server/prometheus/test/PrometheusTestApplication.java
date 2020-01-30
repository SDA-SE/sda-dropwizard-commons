package org.sdase.commons.server.prometheus.test;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import org.sdase.commons.client.jersey.ClientFactory;
import org.sdase.commons.client.jersey.JerseyClientBundle;
import org.sdase.commons.server.prometheus.PrometheusBundle;
import org.sdase.commons.shared.tracing.ConsumerTracing;

@Path("/")
public class PrometheusTestApplication extends Application<Configuration> {
  private JerseyClientBundle<Configuration> jerseyClientBundle =
      JerseyClientBundle.builder().build();
  private Client myClient;

  @Override
  public void initialize(final Bootstrap<Configuration> bootstrap) {
    bootstrap.addBundle(jerseyClientBundle);

    bootstrap.addBundle(PrometheusBundle.builder().build());
  }

  @Override
  public void run(Configuration config, final Environment environment) {
    environment.jersey().register(this);

    environment
        .healthChecks()
        .register(
            "anUnhealthyCheck",
            new HealthCheck() {
              @Override
              protected Result check() {
                return HealthCheck.Result.unhealthy("always unhealthy");
              }
            });

    environment
        .healthChecks()
        .register(
            "aHealthyCheck",
            new HealthCheck() {
              @Override
              protected Result check() {
                return HealthCheck.Result.healthy("always healthy");
              }
            });

    // dummy implementation of the consumer token filter
    environment
        .jersey()
        .register(
            (ContainerRequestFilter)
                requestContext ->
                    requestContext.setProperty(
                        ConsumerTracing.NAME_ATTRIBUTE,
                        requestContext.getHeaders().getFirst("Consumer-Name")));

    ClientFactory clientFactory = jerseyClientBundle.getClientFactory();
    myClient = clientFactory.externalClient().buildGenericClient("myClient");
  }

  @GET
  @Path("/ping")
  public Response pingResource() {
    return Response.ok("pong").build();
  }

  @GET
  @Path("/client/{port}")
  public Response testClient(@PathParam("port") int port) {
    myClient.target(String.format("http://localhost:%d/ping", port)).request().get();
    return Response.ok("client").build();
  }

  @GET
  @Path("/path/{param}")
  public Response pathWithSegment(@PathParam("param") String pathParam) {
    return Response.ok(pathParam).build();
  }
}
