package org.sdase.commons.client.jersey.clients.apia;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

public interface ApiA {

  @GET
  @Path("/cars")
  @Produces(MediaType.APPLICATION_JSON)
  List<Car> getCars();
}
