package org.sdase.commons.server.opentelemetry.decorators;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.COOKIE;
import static javax.ws.rs.core.HttpHeaders.SET_COOKIE;
import static org.apache.commons.lang3.StringUtils.join;

import io.opentelemetry.api.common.AttributeKey;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

public class HeadersUtils {

  private HeadersUtils() {
    // prevent instances
  }

  /**
   * Please use {@link HeadersUtils#convertHeadersToString(MultivaluedMap)} to generate values for
   * this tag.
   */
  public static final AttributeKey<List<String>> HTTP_REQUEST_HEADERS =
      AttributeKey.stringArrayKey("http.request_headers");

  /**
   * Please use {@link HeadersUtils#convertHeadersToString(MultivaluedMap)} to generate values for
   * this tag.
   */
  public static final AttributeKey<List<String>> HTTP_RESPONSE_HEADERS =
      AttributeKey.stringArrayKey("http.response_headers");

  /**
   * Convert a given {@link MultivaluedMap} with {@link String} keys to the format [key0 = 'value0',
   * 'value1']; [key1 = 'value2']; ...
   *
   * @param headers The {@link MultivaluedMap} with {@link String} keys
   * @return Formatted {@link String} of header keys and values or {@code null}, if {@code null} was
   *     passed as parameter.
   */
  public static List<String> convertHeadersToString(MultivaluedMap<String, ?> headers) {
    if (headers != null) {
      MultivaluedMap<String, ?> sanitizedHeaders = sanitizeHeaders(headers);

      return sanitizedHeaders.entrySet().stream()
          .map(
              entry ->
                  join(
                      "[",
                      entry.getKey(),
                      " = '",
                      entry.getValue().stream()
                          .map(Object::toString)
                          .collect(Collectors.joining("', '")),
                      "']"))
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  public static <T> MultivaluedMap<String, T> sanitizeHeaders(MultivaluedMap<String, T> headers) {
    MultivaluedMap<String, T> sanitizedHeaders = new MultivaluedHashMap<>();

    headers.forEach(
        (key, value) -> {
          if (AUTHORIZATION.equalsIgnoreCase(key)) {
            sanitizedHeaders.put(
                key,
                value.stream()
                    .map(h -> (T) sanitizeAuthorizationHeader(h.toString()))
                    .collect(Collectors.toList()));
          } else if (SET_COOKIE.equalsIgnoreCase(key) || COOKIE.equalsIgnoreCase(key)) {
            sanitizedHeaders.putSingle(key, (T) "?");
          } else {
            sanitizedHeaders.put(key, value);
          }
        });

    return sanitizedHeaders;
  }

  private static String sanitizeAuthorizationHeader(String header) {
    if (header.startsWith("Bearer ")) {
      return "Bearer ?";
    } else {
      return "?";
    }
  }
}
