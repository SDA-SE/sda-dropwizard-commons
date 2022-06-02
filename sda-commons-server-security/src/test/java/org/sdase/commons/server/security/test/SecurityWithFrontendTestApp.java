package org.sdase.commons.server.security.test;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.dropwizard.bundles.SystemPropertyAndEnvironmentLookup;
import org.sdase.commons.server.jackson.JacksonConfigurationBundle;
import org.sdase.commons.server.security.SecurityBundle;

@Path("")
public class SecurityWithFrontendTestApp extends Application<Configuration> {

  public static void main(String[] args) throws Exception {
    new SecurityWithFrontendTestApp().run(args);
  }

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    SystemPropertyAndEnvironmentLookup lookup = new SystemPropertyAndEnvironmentLookup();
    bootstrap.addBundle(
        ConfigurationSubstitutionBundle.builder().build()); // needed to update the config in tests
    if (!"true".equals(lookup.lookup("DISABLE_BUFFER_CHECK"))) {
      bootstrap.addBundle(SecurityBundle.builder().withFrontendSupport().build());
    } else {
      bootstrap.addBundle(
          SecurityBundle.builder().disableBufferLimitValidation().withFrontendSupport().build());
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
