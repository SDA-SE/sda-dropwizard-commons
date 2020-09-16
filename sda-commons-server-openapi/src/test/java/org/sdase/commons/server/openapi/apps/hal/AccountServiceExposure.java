/*
 * Copyright (c) 2017 Open API Tools
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Based on https://github.com/openapi-tools/swagger-hal/blob/05c00c9d5734731a1d08b4b43e0156279629a08d/src/test/java/io/openapitools/hal/example/AccountServiceExposure.java
 */
package org.sdase.commons.server.openapi.apps.hal;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.sdase.commons.server.openapi.apps.hal.model.AccountRepresentation;
import org.sdase.commons.server.openapi.apps.hal.model.AccountUpdateRepresentation;
import org.sdase.commons.server.openapi.apps.hal.model.AccountsRepresentation;

/** Exposing account as REST service. */
@Path("/accounts")
@SecurityRequirement(name = "oauth2")
@Schema()
public class AccountServiceExposure {

  @GET
  @Produces({"application/hal+json"})
  @Operation(summary = "List all accounts")
  @ApiResponse(
      responseCode = "200",
      description = "List all accounts",
      content =
          @Content(
              mediaType = APPLICATION_JSON,
              schema = @Schema(implementation = AccountsRepresentation.class)))
  public Response list(@Context UriInfo uriInfo, @Context Request request) {
    return Response.ok().build();
  }

  @GET
  @Path("{regNo}-{accountNo}")
  @Produces({"application/hal+json"})
  @Operation(summary = "Get single account")
  @ApiResponse(
      responseCode = "200",
      description = "Get single account",
      content =
          @Content(
              mediaType = APPLICATION_JSON,
              schema = @Schema(implementation = AccountsRepresentation.class)))
  @ApiResponse(responseCode = "404", description = "No account found.")
  public Response get(
      @PathParam("regNo") @Pattern(regexp = "^[0-9]{4}$") String regNo,
      @PathParam("accountNo") @Pattern(regexp = "^[0-9]+$") String accountNo,
      @Context UriInfo uriInfo,
      @Context Request request) {
    return Response.ok().build();
  }

  @PUT
  @Path("{regNo}-{accountNo}")
  @Produces({"application/hal+json"})
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(summary = "Create new or update existing account")
  @ApiResponse(
      responseCode = "200",
      description = "Create new or update existing account",
      content =
          @Content(
              mediaType = APPLICATION_JSON,
              schema = @Schema(implementation = AccountRepresentation.class)))
  @ApiResponse(responseCode = "400", description = "No updating possible")
  public Response createOrUpdate(
      @PathParam("regNo") @Pattern(regexp = "^[0-9]{4}$") String regNo,
      @PathParam("accountNo") @Pattern(regexp = "^[0-9]+$") String accountNo,
      @Valid AccountUpdateRepresentation account,
      @Context UriInfo uriInfo,
      @Context Request request) {
    return Response.ok().build();
  }
}
