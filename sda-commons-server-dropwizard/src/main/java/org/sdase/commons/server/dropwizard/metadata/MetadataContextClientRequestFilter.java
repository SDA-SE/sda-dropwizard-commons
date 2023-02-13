package org.sdase.commons.server.dropwizard.metadata;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import org.apache.commons.lang3.StringUtils;

/**
 * A {@link ClientRequestFilter} to submit the {@link MetadataContext} to other services that are
 * called synchronously.
 */
public class MetadataContextClientRequestFilter implements ClientRequestFilter {

  private final Set<String> metadataFields;

  public MetadataContextClientRequestFilter(Set<String> metadataFields) {
    this.metadataFields = metadataFields;
  }

  @Override
  public void filter(ClientRequestContext requestContext) throws IOException {
    if (!metadataFields.isEmpty()) {
      MetadataContext metadataContext = MetadataContext.current();
      var headers = requestContext.getHeaders();
      for (String metadataField : metadataFields) {
        List<String> valuesByKey = metadataContext.valuesByKey(metadataField);
        if (valuesByKey == null) {
          continue;
        }
        valuesByKey.stream()
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .distinct()
            .forEach(v -> headers.add(metadataField, v));
      }
    }
  }
}
