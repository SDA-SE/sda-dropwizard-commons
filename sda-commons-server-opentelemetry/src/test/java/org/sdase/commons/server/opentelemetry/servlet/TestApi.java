package org.sdase.commons.server.opentelemetry.servlet;

import java.net.URI;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
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
