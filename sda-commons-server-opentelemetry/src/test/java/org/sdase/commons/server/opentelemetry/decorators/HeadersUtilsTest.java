package org.sdase.commons.server.opentelemetry.decorators;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.COOKIE;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.HttpHeaders.SET_COOKIE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sdase.commons.server.opentelemetry.decorators.HeadersUtils.convertHeadersToString;
import static org.sdase.commons.server.opentelemetry.decorators.HeadersUtils.sanitizeHeaders;

import java.util.List;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;

class HeadersUtilsTest {

  @Test
  void shouldConvertSingleHeaderToString() {
    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
    headers.put(LOCATION, singletonList("1"));

    List<String> formattedHeaders = convertHeadersToString(headers);

    assertThat(formattedHeaders).containsExactly("[Location = '1']");
  }

  @Test
  void shouldConvertMultipleHeadersToString() {
    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
    headers.put(LOCATION, singletonList("1"));
    headers.put("Transfer-Encoding", singletonList("2"));

    List<String> formattedHeaders = convertHeadersToString(headers);

    assertThat(formattedHeaders).containsExactly("[Transfer-Encoding = '2']", "[Location = '1']");
  }

  @Test
  void shouldConvertMultiValueHeaderToString() {
    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
    headers.put(LOCATION, asList("1", "2"));

    List<String> formattedHeaders = convertHeadersToString(headers);

    assertThat(formattedHeaders).containsExactly("[Location = '1', '2']");
  }

  @Test
  void shouldReturnEmptyStringIfNoHeaders() {
    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

    List<String> formattedHeaders = convertHeadersToString(headers);

    assertThat(formattedHeaders).isEmpty();
  }

  @Test
  void shouldSanitizeSetCookieHeader() {
    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
    headers.put(SET_COOKIE, singletonList("1234"));
    MultivaluedMap<String, ?> sanitizedHeaders = sanitizeHeaders(headers);

    assertThat(sanitizedHeaders.getFirst(SET_COOKIE)).isEqualTo("?");
  }

  @Test
  void shouldSanitizeCookieHeader() {
    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
    headers.put(COOKIE, singletonList("1234"));
    MultivaluedMap<String, ?> sanitizedHeaders = sanitizeHeaders(headers);

    assertThat(sanitizedHeaders.getFirst(COOKIE)).isEqualTo("?");
  }

  @Test
  void shouldSanitizeAuthorizationHeader() {
    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
    headers.put(AUTHORIZATION, singletonList("1234"));
    MultivaluedMap<String, ?> sanitizedHeaders = sanitizeHeaders(headers);

    assertThat(sanitizedHeaders.getFirst(AUTHORIZATION)).isEqualTo("?");
  }

  @Test
  void shouldSanitizeBearerTokenInAuthorizationHeader() {
    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
    headers.put(AUTHORIZATION, singletonList("Bearer eyXXX.yyy.zzz"));
    MultivaluedMap<String, ?> sanitizedHeaders = sanitizeHeaders(headers);

    assertThat(sanitizedHeaders.getFirst(AUTHORIZATION)).isEqualTo("Bearer ?");
  }
}
