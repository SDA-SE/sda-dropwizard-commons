package org.sdase.commons.server.jackson;

import io.dropwizard.core.setup.Environment;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper utility that can be registered in the Jersey environment to be injected into services. It
 * supports checking if the client requested to embed a linked resource. Usage:
 *
 * <pre>
 *   if (embedHelper.isEmbeddingOfRelationRequested("owner")) {
 *     carResource.setOwner(createPerson(ownerId));
 *   }
 * </pre>
 *
 * <p>Clients may request to resolve a link as an embedded resource by appending the query param
 * {@code embed} to the request uri containing all the named relations they want to embed. {@code
 * embed} may be be added multiple times to create an array of relation names or contain multiple
 * relation names separated by comma, e.g.:
 *
 * <ul>
 *   <li>{@code GET /api/cars?embed=drivers&embed=owner}
 *   <li>{@code GET /api/cars?embed=drivers,owner}
 * </ul>
 */
public class EmbedHelper implements Feature {

  private static final String EMBED_QUERY_PARAM = "embed";

  @Context UriInfo uriInfo;

  /**
   * Create a new embed helper instance and register it in the given {@code environment} so that it
   * becomes {@link Context} aware.
   *
   * @param environment The Dropwizard environment to register this instance in.
   */
  public EmbedHelper(Environment environment) {
    environment.jersey().register(this);
  }

  /**
   * @param relationName the name of the relation, e.g. {@code project} in @{code { "_links":
   *     {"project": {"href": "http..."}}}}
   * @return if the client requested to embed the resource referenced in the link with the given
   *     relation name
   */
  public boolean isEmbeddingOfRelationRequested(String relationName) {
    return identifyRequestedEmbeddedFields(uriInfo.getQueryParameters()).contains(relationName);
  }

  private static Set<String> identifyRequestedEmbeddedFields(
      MultivaluedMap<String, String> queryParameters) {
    if (queryParameters == null) {
      return Collections.emptySet();
    }
    List<String> embed = queryParameters.get(EMBED_QUERY_PARAM);
    if (embed == null) {
      return Collections.emptySet();
    }
    return embed.stream()
        .filter(Objects::nonNull)
        .map(s -> s.split(","))
        .flatMap(Stream::of)
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toSet());
  }

  @Override
  public boolean configure(FeatureContext context) {
    // nothing to do here, registration is done at time of construction
    // this class implements Feature to avoid a warning in Jersey
    return true;
  }
}
