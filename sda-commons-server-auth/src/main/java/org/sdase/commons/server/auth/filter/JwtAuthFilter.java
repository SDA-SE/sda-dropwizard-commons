package org.sdase.commons.server.auth.filter;

import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.Authenticator;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

/**
 * A dropwizard authentication filter using JSON Web Token (JWT). The filter
 * checks if JWT (Json Web Token) sent by the user is a valid signed JWT to
 * access a protected route or resource, the user agent should send the JWT,
 * typically in the Authorization header using the Bearer schema. The content of
 * the header should look like the following: Authorization: Bearer <token>
 * 
 *
 * @param <P> The type of the principal.
 */
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthFilter<P extends Principal> extends AuthFilter<Optional<String>, P> {

   private static final String AUTHENTICATION_SCHEME_BEARER = "Bearer";

   private JwtAuthFilter() {
   }

   @Override
   public void filter(ContainerRequestContext requestContext) {
      final MultivaluedMap<String, String> headers = requestContext.getHeaders();
      final String jwt = extractAuthorizationToken(headers);

      if (!authenticate(requestContext, Optional.ofNullable(jwt), SecurityContext.BASIC_AUTH)) {
         throw new WebApplicationException(unauthorizedHandler.buildResponse(prefix, realm));
      }
   }


   // builder

   /**
    * Builder for {@link JwtAuthFilter}.
    * <p>
    * An {@link Authenticator} must be provided during the building process.
    * </p>
    *
    * @param <P>
    *           the type of the principal
    */
   public static class Builder<P extends Principal> extends AuthFilterBuilder<Optional<String>, P, JwtAuthFilter<P>> {

      @Override
      protected JwtAuthFilter<P> newInstance() {
         return new JwtAuthFilter<>();
      }
   }

   private String extractAuthorizationToken(MultivaluedMap<String, String> headers) {

      final List<String> authorization = headers.get(HttpHeaders.AUTHORIZATION);
      if (authorization == null || authorization.isEmpty()) {
         return null;
      }
      String authorizationHeader = authorization.get(0);
      if (!authorizationHeader.contains(AUTHENTICATION_SCHEME_BEARER)) {
         return null;
      }
      return authorizationHeader.replaceFirst(AUTHENTICATION_SCHEME_BEARER + " ", "");

   }

}
