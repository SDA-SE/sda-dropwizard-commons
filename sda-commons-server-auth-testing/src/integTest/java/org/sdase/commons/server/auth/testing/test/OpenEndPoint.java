package org.sdase.commons.server.auth.testing.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/open")
public class OpenEndPoint {
   @GET
   public Response getOpenData() {
      return Response.ok("We are open.").build();
   }
}
