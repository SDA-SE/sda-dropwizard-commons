package org.sdase.commons.server.dropwizard.metadata;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

/**
 * A filter that creates the {@link MetadataContext} for processing a request. The {@link
 * MetadataContext} is based on the header fields of the incoming request and includes the
 * configured {@linkplain #MetadataContextFilter(Set) fields}.
 */
@Priority(Priorities.USER)
public class MetadataContextFilter implements ContainerRequestFilter, ContainerResponseFilter {

  private final Set<String> metadataFields;

  public MetadataContextFilter(Set<String> metadataFields) {
    this.metadataFields = metadataFields;
  }

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    var metadataContext = buildContext(requestContext.getHeaders());
    MetadataContextHolder.set(metadataContext.toMetadataContext());
  }

  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext)
      throws IOException {
    MetadataContextHolder.clear();
  }

  private DetachedMetadataContext buildContext(MultivaluedMap<String, String> headers) {
    var metadataContext = new DetachedMetadataContext();
    for (String metadataField : metadataFields) {
      List<String> values = normalize(headers.get(metadataField));
      metadataContext.put(metadataField, values);
    }
    return metadataContext;
  }

  private List<String> normalize(List<String> original) {
    if (original == null) {
      return List.of();
    }
    return original.stream()
        .map(s -> s.split(","))
        .flatMap(Stream::of)
        .filter(StringUtils::isNotBlank)
        .map(String::trim)
        .toList();
  }
}
