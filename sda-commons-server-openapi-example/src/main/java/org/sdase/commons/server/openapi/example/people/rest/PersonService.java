package org.sdase.commons.server.openapi.example.people.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import org.sdase.commons.shared.api.error.ApiError;

// Identify this interface for Swagger documentation.
//
// We prefer to put these annotations into a separate interface to make the code of the endpoint
// more readable because it contains less boilerplate code.
//
// Requires both a Authorization and a Consumer-Token. All requests inside are tagged as "People",
// tags help to group the documentation for better navigation.
@SecurityRequirement(name = AuthDefinition.BEARER_TOKEN)
@SecurityRequirement(name = AuthDefinition.CONSUMER_TOKEN)
@Tag(name = "People")
@Path("people") // define the base path of this endpoint
@Produces({
  APPLICATION_JSON,
  "application/hal+json"
}) // should be set to produce 406 for other accept headers
@Consumes({APPLICATION_JSON}) // should be set to produce 415 for other Content-Type headers
public interface PersonService {
  @GET
  @Operation(
      summary = "Returns all people stored in this service.") // Set a description for the operation
  @ApiResponse(
      responseCode = "200",
      description = "Returns the requested document.",
      content =
          @Content(array = @ArraySchema(schema = @Schema(implementation = PersonResource.class))))
  List<PersonResource> findAllPeople();

  @POST
  @Operation(
      summary = "Creates a new person.",
      description =
          "The following default values are created on service start-up:\n\n"
              + "| Firstname | Lastname   |\n"
              + "|-----------|------------|\n"
              + "| Max       | Mustermann |\n"
              + "| John      | Doe        |\n\n"
              + "You have **full** markdown support here!")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "The new person was created and the uri of the person is returned.",
            // It is also possible to define the response headers for a given response
            headers = {
              @Header(
                  name = "Location",
                  description = "The location of the new person",
                  schema = @Schema(implementation = String.class))
            })
      })
  Response createPerson(CreatePersonResource person);

  @GET
  @Path("{personId}")
  @Operation(summary = "Returns a single person by id.")
  // It is also possible to define multiple responses that might occur:
  @ApiResponse(
      responseCode = "200",
      description = "Returns the requested person.",
      content = @Content(schema = @Schema(implementation = PersonResource.class)))
  @ApiResponse(
      responseCode = "404",
      description = "The requested person was not found.",
      content = @Content(schema = @Schema(implementation = ApiError.class)))
  PersonResource findPersonById(
      @Parameter(
              description = "The id of the person to request.",
              required = true) // Document path params
          @PathParam("personId")
          int personId);
}
