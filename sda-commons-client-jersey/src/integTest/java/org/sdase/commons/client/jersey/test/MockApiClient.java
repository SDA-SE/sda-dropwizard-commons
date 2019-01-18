package org.sdase.commons.client.jersey.test;

import org.sdase.commons.client.jersey.ApiClientTest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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

   @GET
   @Path("/cars/{id}")
   @Produces(MediaType.APPLICATION_JSON)
   Car getCar(@PathParam("id") String id);

   @GET
   @Path("/cars/{id}")
   @Produces(MediaType.APPLICATION_JSON)
   Car getCarWithFilteredFields(@PathParam("id") String id, @QueryParam("fields") List<String> selectedFields);

   @GET
   @Path("/cars/{id}")
   @Produces(MediaType.APPLICATION_JSON)
   Response getCarResponse(@PathParam("id") String id);

   @POST
   @Path("/cars")
   @Consumes(MediaType.APPLICATION_JSON)
   Response createCar(Car newCar);

   // NOT supported yet!
   default Response getLightBlueCar() {
      return getCarResponse(ApiClientTest.LIGHT_BLUE_CAR.getSign());
   }

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
