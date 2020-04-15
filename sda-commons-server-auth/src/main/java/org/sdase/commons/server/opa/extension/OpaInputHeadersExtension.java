package org.sdase.commons.server.opa.extension;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * An input extension that adds the request headers to the {@link
 * org.sdase.commons.server.opa.filter.model.OpaInput}.
 *
 * <p>Header names are defined as case-insensitive by the HTTP RFC (<a
 * href="https://tools.ietf.org/html/rfc7230#section-3.2">RFC 7230 Sec. 3.2</a>). This filter
 * normalizes all headers to lowercase and included as:
 *
 * <pre>
 *   …,
 *   "headers": {
 *     "my-header": ["value0", "value1", "value2"],
 *     "another-header": ["value0,value1", "value2,value3"]
 *   }
 * </pre>
 *
 * <p>Headers are currently passed to OPA as read by the framework. There might be an issue with
 * multivalued headers. The representation differs depending on how the client sends the headers. It
 * might be a list with values, or one entry separated with a separator, for example ',' or a
 * combination of both.
 */
public class OpaInputHeadersExtension implements OpaInputExtension<MultivaluedMap<String, String>> {

  //
  // Builder
  //
  public static ExtensionBuilder builder() {
    return new OpaInputHeadersExtension.Builder();
  }

  private OpaInputHeadersExtension() {
    // private method to prevent external instantiation
  }

  @Override
  public MultivaluedMap<String, String> createAdditionalInputContent(
      ContainerRequestContext requestContext) {
    return lowercaseHeaderNames(requestContext.getHeaders());
  }

  /**
   * Lowercase header names. In HTTP RFC, the header names are defined as case-insensitive, so a
   * normalization is needed to define how the headers are named in OPA.
   *
   * @param headers request headers
   * @return Map with normalized header names
   */
  private MultivaluedMap<String, String> lowercaseHeaderNames(
      MultivaluedMap<String, String> headers) {
    MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
    headers.forEach((key, value) -> result.addAll(key.toLowerCase(), value));
    return result;
  }

  public interface ExtensionBuilder {
    OpaInputHeadersExtension build();
  }

  public static class Builder implements ExtensionBuilder {

    private Builder() {
      // private method to prevent external instantiation
    }

    @Override
    public OpaInputHeadersExtension build() {
      return new OpaInputHeadersExtension();
    }
  }
}
