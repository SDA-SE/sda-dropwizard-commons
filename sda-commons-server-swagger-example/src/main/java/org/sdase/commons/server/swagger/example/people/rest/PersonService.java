package org.sdase.commons.server.swagger.example.people.rest;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sdase.commons.server.swagger.example.people.rest.AuthDefinition.BEARER_TOKEN;
import static org.sdase.commons.server.swagger.example.people.rest.AuthDefinition.CONSUMER_TOKEN;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.ResponseHeader;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

// Identify this interface for Swagger documentation.
//
// We prefer to put these annotations into a separate interface to make the code of the endpoint
// more readable because it contains less boilerplate code.
//
// Requires both a Authorization and a Consumer-Token. All requests inside are tagged as "People",
// tags help to group the documentation for better navigation.
@Api(authorizations = {@Authorization(BEARER_TOKEN), @Authorization(CONSUMER_TOKEN)}, tags = "People")
@Path("people") // define the base path of this endpoint
@Produces({APPLICATION_JSON, "application/hal+json"}) // should be set to produce 406 for other accept headers
@Consumes({APPLICATION_JSON}) // should be set to produce 406 for other accept headers
public interface PersonService {
   @GET
   @ApiOperation(value = "Returns all people stored in this service.") // Set a description for the operation
   @ApiResponses({
         // Define all possible responses, with their status code and documentation text
         @ApiResponse(code = HTTP_OK, message = "Returns the requested document.")})
   List<PersonResource> findAllPeople();

   @POST
   @ApiOperation(
         value = "Creates a new person.",
         notes = "The following default values are created on service start-up:\n\n"
             + "| Firstname | Lastname   |\n"
             + "|-----------|------------|\n"
             + "| Max       | Mustermann |\n"
             + "| John      | Doe        |\n\n"
             + "You have **full** markdown support here!")
   @ApiResponses(value = {
         @ApiResponse(
               code = HTTP_CREATED,
               message = "The new person was created and the uri of the person is returned.",
               // It is also possible to define the response headers for a given
               // response
               responseHeaders = {
                     @ResponseHeader(
                           name = "Location",
                           description = "The location of the new person",
                           response = String.class)
         })})
   Response createPeople(PersonResource person);

   @GET
   @Path("{personId}")
   @ApiOperation(value = "Returns a single person by id.")
   @ApiResponses({
         // It is also possible to define multiple responses that might occur:
         @ApiResponse(code = HTTP_OK, message = "Returns the requested person."),
         @ApiResponse(code = HTTP_NOT_FOUND, message = "The requested person was not found.") })
   PersonResource findPersonById(
         @ApiParam(value = "The id of the person to request.", required = true) // Document path params
         @PathParam("personId") int personId);
}
