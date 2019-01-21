package org.sdase.commons.server.swagger.example.people.rest;

import static io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation.HEADER;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.swagger.annotations.ApiKeyAuthDefinition;
import io.swagger.annotations.SecurityDefinition;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;

// Your application might have an additional place were global swagger annotations are defined, like
// the App interface. It's only purpose is do define the annotations in the right scope.
// This part is optional, but you might have to define more than possible using the bundle.
@SwaggerDefinition(
      // Set the consumed and produced media types, should be JSON when creating
      // a service running on the SDA platform.
      consumes = APPLICATION_JSON, produces = APPLICATION_JSON,
      // Set the descriptions for tags referenced elsewhere.
      tags = {
            @Tag(name = "People", description = "API features related to *People*")
      },
      // Define the security definitions that are later referenced inside the api definitions.
      // Here two are defined, one for the consumer token and the access token required for every
      // call.
      securityDefinition = @SecurityDefinition(
            apiKeyAuthDefinitions = {
                  @ApiKeyAuthDefinition(
                        key = AuthDefinition.CONSUMER_TOKEN,
                        name = "Consumer-Token",
                        in = HEADER,
                        description = "Passes the Consumer-Token to the service call."),
                  @ApiKeyAuthDefinition(
                        key = AuthDefinition.BEARER_TOKEN,
                        name = "Authorization",
                        in = HEADER,
                        description = "Passes the Access-Token to the service call. Use a value with the format ```Bearer xxxxxx.yyyyyyy.zzzzzz```") }))
public interface Api {

}
