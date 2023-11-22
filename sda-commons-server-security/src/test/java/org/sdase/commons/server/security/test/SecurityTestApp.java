package org.sdase.commons.server.security.test;

import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.dropwizard.bundles.SystemPropertyAndEnvironmentLookup;
import org.sdase.commons.server.jackson.JacksonConfigurationBundle;
import org.sdase.commons.server.security.SecurityBundle;

@Path("")
public class SecurityTestApp extends Application<Configuration> {

  public static void main(String[] args) throws Exception {
    new SecurityTestApp().run(args);
  }

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    SystemPropertyAndEnvironmentLookup lookup = new SystemPropertyAndEnvironmentLookup();
    bootstrap.addBundle(
        ConfigurationSubstitutionBundle.builder().build()); // needed to update the config in tests
    if (!"true".equals(lookup.lookup("DISABLE_BUFFER_CHECK"))) {
      bootstrap.addBundle(SecurityBundle.builder().build());
    } else {
      bootstrap.addBundle(SecurityBundle.builder().disableBufferLimitValidation().build());
    }
    if (!"true".equals(lookup.lookup("DISABLE_JACKSON_CONFIGURATION"))) {
      bootstrap.addBundle(
          JacksonConfigurationBundle.builder().build()); // enables custom error handlers
    }
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    environment.jersey().register(this);
  }

  @GET
  @Path("caller")
  @Produces("text/plain")
  public String identifyCaller(@Context HttpServletRequest request) {
    return request.getRemoteAddr();
  }

  @GET
  @Path("link")
  @Produces("text/plain")
  public String produceLink(@Context UriInfo uriInfo) {
    return uriInfo.getBaseUri().toASCIIString();
  }

  @GET
  @Path("throw")
  @Produces("application/json")
  public String throwException() {
    // intentionally use RuntimeException to check error handler for the basics
    throw new RuntimeException("An always thrown exception"); // NOSONAR
  }

  @GET
  @Path("404")
  public Response createNotFoundResponse() {
    return Response.status(404).build();
  }

  @GET
  @Path("header")
  public Response addHeaderToResponse(
      @QueryParam("name") String name, @QueryParam("value") String value) {
    if (name != null && !name.trim().isEmpty() && value != null && !value.trim().isEmpty()) {
      return Response.ok().header(name, value).build();
    }
    return Response.ok().build();
  }
}
