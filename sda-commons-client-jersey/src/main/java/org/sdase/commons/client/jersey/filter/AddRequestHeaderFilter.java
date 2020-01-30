package org.sdase.commons.client.jersey.filter;

import java.util.Optional;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

/**
 * A {@link ClientRequestFilter} that adds a Http header if that header is not set yet and the
 * implementation provides a value for the header.
 *
 * <p>This helper is implemented as interface because it is not possible to add multiple filters of
 * the same type. Therefore users have to implement a dedicated filter for each header they want to
 * add. This implementations may be anonymous. Example:
 *
 * <pre>
 *    <code>ClientRequestFilter filter = new AddRequestHeaderFilter() {
 *         {@literal @Override}
 *          public String getHeaderName() {
 *             return ConsumerTracing.TOKEN_HEADER;
 *          }
 *         {@literal @Override}
 *          public Optional<String> getHeaderValue() {
 *             return consumerTokenSupplier.get();
 *          }
 *    };</code>
 * </pre>
 */
public interface AddRequestHeaderFilter extends ClientRequestFilter {

  /**
   * @return the name of the header that should be added to the request, e.g. {@code Authorization}.
   *     Must not be blank
   */
  String getHeaderName();

  /**
   * @return the value of the header that should be set if no such header is present in the request
   *     yet. May return an empty {@link Optional} if no header should be set.
   */
  Optional<String> getHeaderValue();

  @Override
  default void filter(ClientRequestContext requestContext) {
    if (requestContext.getHeaderString(getHeaderName()) != null) {
      return;
    }
    getHeaderValue().ifPresent(v -> requestContext.getHeaders().add(getHeaderName(), v));
  }
}
