package org.sdase.commons.server.auth.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import org.sdase.commons.server.dropwizard.ContextAwareEndpoint;

/** A test application that provides endpoints for key sources to test loading over Http. */
@Path("/")
public class KeyProviderTestApp extends Application<Configuration> implements ContextAwareEndpoint {

  private ObjectMapper objectMapper;

  @Context private UriInfo uriInfo;

  public static void main(String[] args) throws Exception {
    new KeyProviderTestApp().run(args);
  }

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    this.objectMapper = bootstrap.getObjectMapper();
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    environment.jersey().register(this);
  }

  @GET
  @Path("/rsa-key.pem")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response getRsaPemKey() {
    return Response.ok(getClass().getResourceAsStream("/rsa-example.pem")).build();
  }

  @GET
  @Path("/ec-key.pem")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response getEcPemKey() {
    return Response.ok(getClass().getResourceAsStream("/ec-example.pem")).build();
  }

  @GET
  @Path("/jwks")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getJwks() throws IOException {
    URL jwks = getClass().getResource("/jwks.json");
    return Response.ok(objectMapper.readValue(jwks, new TypeReference<Map<String, Object>>() {}))
        .build();
  }

  @GET
  @Path("/.well-known/openid-configuration")
  public Response getopenIdConfig() {
    Map<String, URI> config =
        Collections.singletonMap(
            "jwks_uri",
            uriInfo
                .getBaseUriBuilder()
                .path(KeyProviderTestApp.class)
                .path(KeyProviderTestApp.class, "getJwks")
                .build());
    return Response.ok(config).build();
  }
}
