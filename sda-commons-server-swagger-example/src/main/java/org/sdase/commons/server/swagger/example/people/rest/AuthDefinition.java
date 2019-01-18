package org.sdase.commons.server.swagger.example.people.rest;

// This interface defines a set of constant used for referencing the authentication schemes while
// documenting the API.
public final class AuthDefinition {

   public static final String BEARER_TOKEN = "bearerToken";
   public static final String CONSUMER_TOKEN = "consumerToken";

   private AuthDefinition() {
      // prevent instantiation
   }
}
