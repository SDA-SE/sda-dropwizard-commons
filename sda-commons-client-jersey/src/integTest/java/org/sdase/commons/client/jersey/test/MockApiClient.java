package org.sdase.commons.client.jersey.test;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/api")
public interface MockApiClient {

   @GET
   @Path("/cars")
   @Produces(MediaType.APPLICATION_JSON)
   List<Car> getCars();

   @GET
   @Path("/cars")
   @Produces(MediaType.APPLICATION_JSON)
   Response requestCars();

   @GET
   @Path("/cars")
   @Produces(MediaType.APPLICATION_JSON)
   Response requestCarsWithCustomConsumerToken(@HeaderParam("Consumer-Token") String consumerToken);


   class Car {
      private String sign;
      private String color;

      public String getSign() {
         return sign;
      }

      public Car setSign(String sign) {
         this.sign = sign;
         return this;
      }

      public String getColor() {
         return color;
      }

      public Car setColor(String color) {
         this.color = color;
         return this;
      }
   }
}
