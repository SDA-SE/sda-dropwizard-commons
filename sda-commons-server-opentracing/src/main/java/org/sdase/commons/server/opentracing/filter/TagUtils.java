package org.sdase.commons.server.opentracing.filter;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;

import io.opentracing.tag.StringTag;
import java.util.stream.Collectors;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.commons.lang3.StringUtils;

public class TagUtils {

  private TagUtils() {
    // prevent instances
  }

  /**
   * Please use {@link TagUtils#convertHeadersToString(javax.ws.rs.core.MultivaluedMap)} to generate
   * values for this tag.
   */
  public static final StringTag HTTP_REQUEST_HEADERS = new StringTag("http.request_headers");

  /**
   * Please use {@link TagUtils#convertHeadersToString(javax.ws.rs.core.MultivaluedMap)} to generate
   * values for this tag.
   */
  public static final StringTag HTTP_RESPONSE_HEADERS = new StringTag("http.response_headers");

  /**
   * Convert a given {@link MultivaluedMap} with {@link String} keys to the format [key0 = 'value0',
   * 'value1']; [key1 = 'value2']; ...
   *
   * @param headers The {@link MultivaluedMap} with {@link String} keys
   * @return Formatted {@link String} of header keys and values or <code>null</code>, if <code>null
   *     </code> was passed as parameter.
   */
  public static String convertHeadersToString(MultivaluedMap<String, ?> headers) {
    if (headers != null) {
      MultivaluedMap<String, ?> sanitizedHeaders = sanitizeHeaders(headers);

      return sanitizedHeaders.entrySet().stream()
          .map(
              entry ->
                  StringUtils.join(
                      "[",
                      entry.getKey(),
                      " = '",
                      entry.getValue().stream()
                          .map(Object::toString)
                          .collect(Collectors.joining("', '")),
                      "']"))
          .collect(Collectors.joining("; "));
    }
    return null;
  }

  public static <T> MultivaluedMap<String, T> sanitizeHeaders(MultivaluedMap<String, T> headers) {
    MultivaluedMap<String, T> sanitizedHeaders = new MultivaluedHashMap<>();

    headers.forEach(
        (key, value) -> {
          if (key.equalsIgnoreCase(AUTHORIZATION)) {
            sanitizedHeaders.put(
                key,
                value.stream()
                    .map(h -> (T) sanitizeAuthorizationHeader(h.toString()))
                    .collect(Collectors.toList()));
          } else {
            sanitizedHeaders.put(key, value);
          }
        });

    return sanitizedHeaders;
  }

  private static String sanitizeAuthorizationHeader(String header) {
    if (header.startsWith("Bearer ")) {
      return "Bearer ...";
    } else {
      return "...";
    }
  }
}
