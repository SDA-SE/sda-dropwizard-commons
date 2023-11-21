package org.sdase.commons.client.jersey.test;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.sdase.commons.client.jersey.ApiClientTest;
import org.sdase.commons.client.jersey.error.ClientRequestException;
import org.sdase.commons.shared.api.error.ApiException;

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
  Car getCarWithFilteredFields(
      @PathParam("id") String id, @QueryParam("fields") List<String> selectedFields);

  @GET
  @Path("/cars/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  Response getCarResponse(@PathParam("id") String id);

  @POST
  @Path("/cars")
  @Consumes(MediaType.APPLICATION_JSON)
  Response createCar(Car newCar);

  @POST
  @Path("/multi-part")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  Response sendMultiPart(FormDataMultiPart multiPart);

  default Response getLightBlueCar() {
    return getCarResponse(ApiClientTest.LIGHT_BLUE_CAR.getSign());
  }

  default Car getCarOrHandleError(String sign) {
    try {
      return getCar(sign);
    } catch (ClientRequestException e) {
      e.close();
      if (e.getResponse().isPresent()) {
        int status = e.getResponse().get().getStatus();
        if (status == 404) {
          return null;
        }
        String statusAsString = "" + status;
        throw ApiException.builder()
            .httpCode(500)
            .title("Failed to get car with sign " + sign)
            .cause(e)
            .detail("error", "Remote caused HTTP error", "REMOTE_HTTP_ERROR")
            .detail("responseCode", "Remote responded " + statusAsString, statusAsString)
            .build();
      } else if (e.isTimeout()) {
        throw ApiException.builder()
            .httpCode(500)
            .title("Failed to get car with sign " + sign)
            .cause(e)
            .detail("error", "Remote caused timeout", "REMOTE_TIMEOUT")
            .build();
      } else if (e.isProcessingError()) {
        throw ApiException.builder()
            .httpCode(500)
            .title("Failed to get car with sign " + sign)
            .cause(e)
            .detail("error", "Remote sent unknown data", "REMOTE_PROCESSING_ERROR")
            .build();
      } else {
        throw ApiException.builder()
            .httpCode(500)
            .title("Failed to get car with sign " + sign)
            .cause(e)
            .detail("error", "Unknown error occurred when calling remote", "REMOTE_UNKNOWN_ERROR")
            .build();
      }
    }
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
