package org.sdase.commons.server.openapi.example.people.rest;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;

// Your application might have an additional place were global swagger annotations are defined, like
// the App interface. It's only purpose is do define the annotations in the right scope.
@OpenAPIDefinition(
    info =
        @Info(
            // Set the name of the service as title of the Swagger documentation.
            title = "Swagger Example Application",
            // Set the description of the API, like an introduction. Like in most of the fields you
            // may use markdown here to apply custom formatting.
            description =
                "This is the API documentation for the **People API**.\n\n"
                    + "The API provides operations for managing and searching people.",
            // Set the version of the API, which is a string. By default version 1 is used.
            version = "3",
            // Set the contact information of the API author.
            contact =
                @Contact(
                    name = "SDA SE",
                    email = "info@example.com",
                    url = "https://myfuture.sda-se.com"),

            // Set the name of the license of the API as well as an url to the full license
            // information. Note the license is only an example.
            license = @License(name = "MIT", url = "https://opensource.org/licenses/MIT"),
            // For APIs with public availability, terms of service might be required.
            // Set the url to the terms of service here.
            termsOfService = "https://www.sda-se.com/legal-notice/"),
    tags = {@Tag(name = "People", description = "API features related to *People*")})
// Define the security definitions that are later referenced inside the api definitions.
// Here two are defined, one for the consumer token and the access token required for every
// call.
@SecurityScheme(
    // This is an API key (see https://swagger.io/docs/specification/authentication/api-keys/) ...
    type = SecuritySchemeType.APIKEY,
    // ... that is transported in a  header ...
    in = SecuritySchemeIn.HEADER,
    // ... of name 'Consumer-Token' ...
    paramName = "Consumer-Token",
    // ... and it can be referenced from other schemas by this name ...
    name = AuthDefinition.CONSUMER_TOKEN,
    // ... and has a description.
    description = "Passes the Consumer-Token to the service class.")
// This example uses HTTP Bearer token format to document the Authorization header (see
// https://swagger.io/docs/specification/authentication/bearer-authentication/). In some cases, it
// could also be useful to configure OAuth 2.0 as security scheme (see
// https://swagger.io/docs/specification/authentication/oauth2/).
@SecurityScheme(
    // This is a HTTP security scheme that is transported in the Authorization header ...
    type = SecuritySchemeType.HTTP,
    // ... of type Bearer ...
    scheme = "bearer",
    // ... that is documented to be a JWT ...
    bearerFormat = "JWT",
    // ... and it can be referenced from other schemas by this name ...
    name = AuthDefinition.BEARER_TOKEN,
    // ... and has a description.
    description =
        "Passes the Access-Token to the service call. Use a value with the format ```xxxxxx.yyyyyyy.zzzzzz```.")
public interface Api {}
