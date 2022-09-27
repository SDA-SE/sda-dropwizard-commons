package org.sdase.commons.server.jackson.test;

import static java.util.Collections.singletonList;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.openapitools.jackson.dataformat.hal.HALLink;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.assertj.core.util.Lists;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.sdase.commons.server.dropwizard.ContextAwareEndpoint;
import org.sdase.commons.server.jackson.JacksonConfigurationBundle;
import org.sdase.commons.shared.api.error.ApiException;

@Path("")
public class JacksonConfigurationTestApp extends Application<Configuration>
    implements ContextAwareEndpoint {

  @Context private UriInfo uriInfo;

  public static void main(String[] args) throws Exception {
    new JacksonConfigurationTestApp().run(args);
  }

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    bootstrap.addBundle(JacksonConfigurationBundle.builder().build());
    bootstrap.addBundle(new io.dropwizard.forms.MultiPartBundle());
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    environment.jersey().register(this);
  }

  @GET
  @Path("people")
  @Produces(MediaType.APPLICATION_JSON)
  public List<PersonWithChildrenResource> getPeople() {
    return singletonList(getJohnDoeWithChildren());
  }

  @GET
  @Path("people/jdoe")
  @Produces(MediaType.APPLICATION_JSON)
  public PersonResource getJohnDoe() {
    URI self =
        uriInfo.getBaseUriBuilder().path(JacksonConfigurationTestApp.class, "getJohnDoe").build();
    URI address =
        uriInfo
            .getBaseUriBuilder()
            .path(JacksonConfigurationTestApp.class, "getJohnDoe")
            .path("address")
            .path("Hamburg")
            .build();

    return new PersonResource()
        .setFirstName("John")
        .setLastName("Doe")
        .setNickName("Johnny")
        .setSelf(new HALLink.Builder(self).build())
        .setAddress(Lists.newArrayList(new Address().setCity("Hamburg").setId("Hamburg")))
        .setAddressLink(Lists.newArrayList(new HALLink.Builder(address).build()));
  }

  @GET
  @Path("people/jdoe-and-children")
  @Produces(MediaType.APPLICATION_JSON)
  public PersonWithChildrenResource getJohnDoeWithChildren() {
    URI self =
        uriInfo.getBaseUriBuilder().path(JacksonConfigurationTestApp.class, "getJohnDoe").build();
    PersonWithChildrenResource john =
        new PersonWithChildrenResource()
            .setFirstName("John")
            .setLastName("Doe")
            .setNickName("Johnny")
            .setSelf(new HALLink.Builder(self).build());
    john.setChildren(
        singletonList(
            new PersonResource()
                .setFirstName("Yasmine")
                .setLastName("Doe")
                .setNickName("Yassie")
                .setSelf(
                    new HALLink.Builder(uriInfo.getBaseUriBuilder().path("ydoe").build())
                        .build())));
    return john;
  }

  @GET
  @Path("/exception")
  @Produces(MediaType.APPLICATION_JSON)
  public PersonResource getException() {
    throw ApiException.builder()
        .httpCode(400)
        .title("Some exception")
        .detail("parameter", null, "SOME_ERROR_CODE")
        .build();
  }

  @GET
  @Path("/runtimeException")
  @Produces(MediaType.APPLICATION_JSON)
  public PersonResource getRuntimeException() {
    throw new RuntimeException("Not to be rendered by default mapper.");
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
      throw new NotAllowedException(
          "Not Allowed method", new RuntimeException("cause"), "moreAllowed", "moreAllowed1");
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
    return Response.created(
            uriInfo
                .getBaseUriBuilder()
                .path(JacksonConfigurationTestApp.class)
                .path(JacksonConfigurationTestApp.class, "createValidationResource")
                .path("1")
                .build())
        .build();
  }

  @POST
  @Valid
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON) // in case of validation error
  @Path("/searchValidation")
  public Response createExtendedValidationResource(@Valid SearchFilterResource searchFilter) {
    return Response.created(
            uriInfo
                .getBaseUriBuilder()
                .path(JacksonConfigurationTestApp.class)
                .path(JacksonConfigurationTestApp.class, "searchValidation")
                .path("1")
                .build())
        .build();
  }

  @GET
  @Valid
  @Path("/requiredQuery")
  @Produces(MediaType.APPLICATION_JSON)
  public Response createRequiredQueryValidationResource(
      @QueryParam("q") @Valid @NotEmpty String searchString) {
    return Response.ok(searchString).build();
  }

  @POST
  @Path("/requiredForm")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createRequiredFormValidationResource(@NotBlank @FormParam("foo") String foo) {
    return Response.created(
            uriInfo
                .getBaseUriBuilder()
                .path(JacksonConfigurationTestApp.class)
                .path(JacksonConfigurationTestApp.class, "createRequiredFormValidationResource")
                .path("1")
                .build())
        .build();
  }

  @POST
  @Path("/requiredFormData")
  @Consumes(MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createRequiredFormDataValidationResource(
      @NotNull @FormDataParam("text") String text,
      @NotNull @Valid @FormDataParam("file") InputStream file,
      @Valid @FormDataParam("file") FormDataContentDisposition fileDisposition) {
    return Response.created(
            uriInfo
                .getBaseUriBuilder()
                .path(JacksonConfigurationTestApp.class)
                .path(JacksonConfigurationTestApp.class, "createRequiredFormDataValidationResource")
                .path("1")
                .build())
        .build();
  }

  @POST
  @Path("/subtypes")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response createSubType(@Valid @NotNull AbstractResource requestBody) {
    return Response.created(URI.create("/subtypes/" + requestBody.getType())).build();
  }

  @POST
  @Path("/subtypesTolerant")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response createSubTypeAllowNull(@Valid AbstractResource requestBody) {
    if (requestBody == null) {
      // real implementations may do some individual error handling or logging here
      throw ApiException.builder().httpCode(422).title("Invalid type").build();
    }
    return Response.created(URI.create("/subtypesTolerant/" + requestBody.getType())).build();
  }
}
