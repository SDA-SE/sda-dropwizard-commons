package org.sdase.commons.server.auth.testing.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/open")
public class OpenEndPoint {
  @GET
  public Response getOpenData() {
    return Response.ok("We are open.").build();
  }
}
