package org.sdase.commons.server.jackson.test;

import org.sdase.commons.server.jackson.JacksonConfigurationBundle;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.openapitools.jackson.dataformat.hal.HALLink;
import org.sdase.commons.server.jackson.errors.ApiException;

import javax.validation.Valid;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Date;

@Path("")
public class JacksonConfigurationTestApp extends Application<Configuration> {

   @Context
   private UriInfo uriInfo;

   public static void main(String[] args) throws Exception {
      new JacksonConfigurationTestApp().run(args);
   }

   @Override
   public void initialize(Bootstrap<Configuration> bootstrap) {
      bootstrap.addBundle(JacksonConfigurationBundle.builder().build());

   }

   @Override
   public void run(Configuration configuration, Environment environment) {
      environment.jersey().register(this);
   }

   @GET
   @Path("/jdoe")
   @Produces(MediaType.APPLICATION_JSON)
   public PersonResource getJohnDoe() {
      URI self = uriInfo.getBaseUriBuilder().path(JacksonConfigurationTestApp.class, "getJohnDoe").build();
      return new PersonResource()
            .setFirstName("John")
            .setLastName("Doe")
            .setNickName("Johnny")
            .setSelf(new HALLink.Builder(self).build());
   }

   @GET
   @Path("/exception")
   @Produces(MediaType.APPLICATION_JSON)
   public PersonResource getException() {
      throw ApiException.builder()
            .httpCode(500)
            .title("Some exception")
            .detail("parameter", null, "MANDATORY_FIELD_IS_MISSING" )
            .build();
   }


   @GET
   @Path("/jaxrsexception")
   @Produces(MediaType.APPLICATION_JSON)
   public PersonResource notFoundException(@QueryParam("type") String type) {
      if ("NotFound".equals(type)) {
         throw new NotFoundException("Something is missing.");
      } else if ("BadRequest".equals(type)) {
         throw new BadRequestException("Bad Request");
      } else if ("Forbidden".equals(type)) {
         throw new ForbiddenException("Forbidden");
      } else if ("NotAcceptable".equals(type)) {
         throw new NotAcceptableException("Not Acceptable");
      } else if ("NotAllowed".equals(type)) {
         throw new NotAllowedException("Not Allowed method", new RuntimeException("cause"), "moreAllowed", "moreAllowed1");
      } else if ("NotAuthorized".equals(type)) {
         throw new NotAuthorizedException("Bearer");
      } else if ("NotSupported".equals(type)) {
         throw new NotSupportedException("NotSupported");
      } else if ("ServiceUnavailable".equals(type)) {
         throw new ServiceUnavailableException("Service unavailable", new Date());
      } else if ("InternalServerError".equals(type)) {
         throw new InternalServerErrorException("Internal Server Error");
      }
      return null;
   }


   @POST
   @Valid
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON) // in case of validation error
   @Path("/validation")
   public Response createValidationResource(@Valid ValidationResource resource) {
      return Response.created(uriInfo.getBaseUriBuilder().path(JacksonConfigurationTestApp.class).path(JacksonConfigurationTestApp.class, "createValidationResource").path("1").build()).build();
   }


}
