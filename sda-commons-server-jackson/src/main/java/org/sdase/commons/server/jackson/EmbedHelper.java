package org.sdase.commons.server.jackson;

import io.dropwizard.setup.Environment;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

/**
 * Helper utility that can be registered in the Jersey environment to be
 * injected into services. It supports checking if the client requested to embed
 * a linked resource. Usage: <code>
 *   if (embedHelper.isEmbeddingOfRelationRequested("owner")) {
 *    carResource.setOwner(createPerson(ownerId));
 *   }
 * </code>
 *
 * <p>
 * Clients may request to resolve a link as an embedded resource by appending
 * the query param {@code embed} to the request uri containing all the named
 * relations they want to embed. {@code
 * embed} may be be added multiple times to create an array of relation names or
 * contain multiple relation names separated by comma, e.g.:
 *
 * <ul>
 * <li>GET /api/cars?embed=drivers&embed=owner
 * <li>GET /api/cars?embed=drivers,owner
 * </ul>
 */
public class EmbedHelper {

   private static final String EMBED_QUERY_PARAM = "embed";

   @Context
   UriInfo uriInfo;

   /**
    * Create a new embed helper instance.
    * 
    * @param environment
    *           The Dropwizard environment to register this instance in.
    */
   public EmbedHelper(Environment environment) {
      environment.jersey().register(this);
   }

   /**
    * @param relationName
    *           the name of the relation, e.g. {@code project} in <code>
    *     {@literal { "_links": {"project": {"href": "http..."}}}}</code>
    * @return if the client requested to embed the resource referenced in the
    *         link with the given relation name
    */
   public boolean isEmbeddingOfRelationRequested(String relationName) {
      return identifyRequestedEmbeddedFields(uriInfo.getQueryParameters()).contains(relationName);
   }

   private static Set<String> identifyRequestedEmbeddedFields(MultivaluedMap<String, String> queryParameters) {
      if (queryParameters == null) {
         return Collections.emptySet();
      }
      List<String> embed = queryParameters.get(EMBED_QUERY_PARAM);
      if (embed == null) {
         return Collections.emptySet();
      }
      return embed
            .stream()
            .filter(Objects::nonNull)
            .map(s -> s.split(","))
            .flatMap(Stream::of)
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
   }
}
