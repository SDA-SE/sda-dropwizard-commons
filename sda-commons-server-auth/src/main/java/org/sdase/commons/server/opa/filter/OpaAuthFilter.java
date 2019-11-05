package org.sdase.commons.server.opa.filter;

import static java.util.stream.Collectors.toList;

import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.Priority;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.Priorities;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import org.apache.commons.lang3.NotImplementedException;
import org.sdase.commons.server.auth.JwtPrincipal;
import org.sdase.commons.server.opa.OpaJwtPrincipal;
import org.sdase.commons.server.opa.config.OpaConfig;
import org.sdase.commons.server.opa.filter.model.OpaRequest;
import org.sdase.commons.server.opa.filter.model.OpaResponse;
import org.sdase.commons.shared.tracing.RequestTracing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * The OpaAuthFilter sends requests to the Open Policy Agent REST API to
 * validate the input against the policy that is configured
 * </p>
 * <p>
 * As default input, the following information are send:
 * <ul>
 *   <li>HTTP method (GET, POST, HEAD,...)</li>
 *   <li>the path as array of strings</li>
 *   <li>the JWT to provide user information (optional)</li>
 *   <li>the trace token to be able to trace the request within the OPA</li>
 * </ul>
 * </p>
 *<p>
 *   OPA response includes at least the general access decision (true/false) and
 *   optional a list of constraints that must be considered during data projection.
 *</p>
 *
 * <p>
 *   Swagger URLs are excluded generally from check
 * </p>
 *
 * <p>
 *   The filter replaces the principal within the security context with a new
 *   {@link OpaJwtPrincipal} that might include a JWT if provided before this filter
 *   as a ({@link JwtPrincipal}.
 * </p>
 */
@Provider
@Priority(Priorities.AUTHORIZATION)
public class OpaAuthFilter implements ContainerRequestFilter {

   private static final Logger LOG = LoggerFactory.getLogger(OpaAuthFilter.class);

   private final WebTarget webTarget;
   private final boolean isDisabled;
   private final List<Pattern> excludePatterns;
   private final ObjectMapper om;

   public OpaAuthFilter(WebTarget webTarget, OpaConfig config, List<String> excludePatterns, ObjectMapper om) {
      this.webTarget = webTarget;
      this.isDisabled = config.isDisableOpa();
      this.excludePatterns = excludePatterns == null ? Collections.emptyList()
            : excludePatterns.stream().map(Pattern::compile).collect(toList());
      this.om = om;
   }

   @Override
   public void filter(ContainerRequestContext requestContext) {

      // collect input parameters for Opa request
      UriInfo uriInfo = requestContext.getUriInfo();
      String method = requestContext.getMethod();
      String trace = requestContext.getHeaderString(RequestTracing.TOKEN_HEADER);
      String jwt = null;

      // if security context already exist and if it is a jwt security context,
      // we include the jwt in the request
      SecurityContext securityContext = requestContext.getSecurityContext();
      Map<String, Claim> claims = null;
      if (null != securityContext) {
         JwtPrincipal jwtPrincipal = getJwtPrincipal(requestContext.getSecurityContext());
         if (jwtPrincipal != null) {
            // JWT principal found, this means that JWT has been validated by
            // auth bundle
            // and can be used within this bundle
            jwt = jwtPrincipal.getJwt();
            claims = jwtPrincipal.getClaims();
         }
      }

      JsonNode constraints = null;
      if (!isDisabled && !isExcluded(uriInfo)) {
         // process the actual request to the open policy agent server
         String[] path = uriInfo.getPathSegments().stream().map(PathSegment::getPath).toArray(String[]::new);
         OpaRequest request = OpaRequest.request(jwt, path, method, trace);
         constraints = authorizeWithOpa(request);
      }

      OpaJwtPrincipal principal = OpaJwtPrincipal.create(jwt, claims, constraints, om);
      replaceSecurityContext(requestContext, securityContext, principal);
   }

   private boolean isExcluded(UriInfo uriInfo) {
      return excludePatterns.stream().anyMatch(p -> p.matcher(uriInfo.getPath()).matches());
   }

   /**
    * replaces the current security context within the request context with a
    * new context providing an OpaJwtPrincipal
    * 
    * @param requestContext
    *           the request context
    * @param securityContext
    *           the original security context
    * @param principal
    *           the new OpaJwtPrincipal
    */
   private void replaceSecurityContext(ContainerRequestContext requestContext, SecurityContext securityContext,
         OpaJwtPrincipal principal) {
      final boolean secure = securityContext != null && securityContext.isSecure();
      final String scheme = securityContext != null ? securityContext.getAuthenticationScheme() : "unknown";

      requestContext.setSecurityContext(new SecurityContext() {
         @Override
         public Principal getUserPrincipal() {
            return principal;
         }

         @Override
         public boolean isUserInRole(String role) {
            throw new NotImplementedException("The isUserInRole methods is not supported for OpaJwtPrincipal");
         }

         @Override
         public boolean isSecure() {
            return secure;
         }

         @Override
         public String getAuthenticationScheme() {
            return scheme;
         }
      });
   }

   private JsonNode authorizeWithOpa(OpaRequest request) {
      OpaResponse resp = null;
      try {
         resp = webTarget.request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(request), OpaResponse.class);
      } catch (WebApplicationException e) {
         try {
            e.getResponse().close();
         } catch(ProcessingException ex) {
            LOG.warn("Error while closing response", ex);
         }
         LOG.warn("Exception when querying OPA. Maybe policy is broken", e);
      } catch (ProcessingException e) {
         LOG.warn("Exception during processing of OPA request.", e);
      }

      if (null == resp || resp.getResult() == null) {
         LOG.warn("Invalid response from OPA. Maybe the policy path or the response format is not correct");
         throw new ForbiddenException("Not authorized");
      }
      if (!resp.isAllow()) {
         throw new ForbiddenException("Not authorized");
      }
      if (null == resp.getResult()) {
         // no constraints defined
         return null;
      }
      return resp.getResult();
   }

   private JwtPrincipal getJwtPrincipal(SecurityContext securityContext) {
      final Principal principal = securityContext.getUserPrincipal();
      return principal instanceof JwtPrincipal ? (JwtPrincipal) principal : null;
   }

}
