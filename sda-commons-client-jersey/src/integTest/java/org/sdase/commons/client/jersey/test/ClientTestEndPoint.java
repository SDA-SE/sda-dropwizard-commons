package org.sdase.commons.client.jersey.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.sdase.commons.client.jersey.ClientFactory;
import org.sdase.commons.client.jersey.test.MockApiClient.Car;

@Path("/api")
public class ClientTestEndPoint {
   private MockApiClient mockApiClient;
   private MockApiClient externalMockApiClient;
   private MockApiClient authMockApiClient;

   ClientTestEndPoint(ClientFactory clientFactory, String baseUrl) {
      mockApiClient = clientFactory
            .platformClient()
            .api(MockApiClient.class, "MockApiClientWithoutAuth")
            .atTarget(baseUrl);
      authMockApiClient = clientFactory
            .platformClient()
            .enableAuthenticationPassThrough()
            .api(MockApiClient.class, "MockApiClientWithAuth")
            .atTarget(baseUrl);
      externalMockApiClient = clientFactory
            .externalClient()
            .api(MockApiClient.class, "MockApiClientExternal")
            .atTarget(baseUrl);
   }

   @GET
   @Path("/cars")
   @Produces(MediaType.APPLICATION_JSON)
   public Response delegate() {
      return mockApiClient.requestCars();
   }

   @GET
   @Path("/cars/{sign}")
   @Produces(MediaType.APPLICATION_JSON)
   public Car delegateGetCar(@PathParam("sign") String sign) {
      return mockApiClient.getCar(sign);
   }

   @GET
   @Path("/carsExternal")
   @Produces(MediaType.APPLICATION_JSON)
   public Response delegateExternal() {
      return externalMockApiClient.requestCars();
   }

   @GET
   @Path("/carsAuth")
   @Produces(MediaType.APPLICATION_JSON)
   public Response delegateWithAuth() {
      return authMockApiClient.requestCars();
   }

}
