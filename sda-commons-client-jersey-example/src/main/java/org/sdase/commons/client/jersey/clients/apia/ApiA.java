package org.sdase.commons.client.jersey.clients.apia;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

public interface ApiA {

  @GET
  @Path("/cars")
  @Produces(MediaType.APPLICATION_JSON)
  List<Car> getCars();
}
