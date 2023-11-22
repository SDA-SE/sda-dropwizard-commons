package org.sdase.commons.server.opentelemetry;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/base/")
public class TestApi {
  private static final Logger LOGGER = LoggerFactory.getLogger(TestApi.class);

  @GET
  @Path("error")
  public String doError() {
    throw new InternalServerErrorException("Something went wrong");
  }

  @GET
  @Path("respond/{value}")
  public String doResponse(@PathParam("value") String value) {
    return value;
  }

  @GET
  @Path("log")
  public void doLog() {
    LOGGER.info("Hello World");
  }

  @POST
  @Path("respond")
  public Response doSave() {
    return Response.created(URI.create("http://sdase/id")).build();
  }

  @GET
  @Path("logError")
  public void doLogError() {
    try {
      throw new IllegalStateException("Never call this method");
    } catch (Exception ex) {
      LOGGER.error("Something went wrong", ex);
    }
  }
}
