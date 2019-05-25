package org.sdase.commons.server.opentracing.filter;

import static com.google.common.net.HttpHeaders.TRANSFER_ENCODING;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sdase.commons.server.opentracing.filter.TagUtils.convertHeadersToString;
import static org.sdase.commons.server.opentracing.filter.TagUtils.sanitizeHeaders;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import org.junit.Test;

public class TagUtilsTest {

  @Test
  public void shouldConvertSingleHeaderToString() {
    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
    headers.put(LOCATION, singletonList("1"));

    String tag = convertHeadersToString(headers);

    assertThat(tag).isEqualTo("[Location = '1']");
  }

  @Test
  public void shouldConvertMultipleHeadersToString() {
    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
    headers.put(LOCATION, singletonList("1"));
    headers.put(TRANSFER_ENCODING, singletonList("2"));

    String tag = convertHeadersToString(headers);

    assertThat(tag).isEqualTo("[Transfer-Encoding = '2']; [Location = '1']");
  }

  @Test
  public void shouldConvertMultiValueHeaderToString() {
    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
    headers.put(LOCATION, asList("1", "2"));

    String tag = convertHeadersToString(headers);

    assertThat(tag).isEqualTo("[Location = '1', '2']");
  }

  @Test
  public void shouldReturnEmptyStringIfNoHeaders() {
    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

    String tag = convertHeadersToString(headers);

    assertThat(tag).isEmpty();
  }

  @Test
  public void shouldReturnNullIfPassedNull() {
    String tag = convertHeadersToString(null);

    assertThat(tag).isNull();
  }

  @Test
  public void shouldSanitizeAuthorizationHeader() {
    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
    headers.put(AUTHORIZATION, singletonList("1234"));
    MultivaluedMap<String, ?> sanitizedHeaders = sanitizeHeaders(headers);

    assertThat(sanitizedHeaders.getFirst(AUTHORIZATION)).isEqualTo("...");
  }

  @Test
  public void shouldSanitizeBearerTokenInAuthorizationHeader() {
    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
    headers.put(AUTHORIZATION, singletonList("Bearer 1234"));
    MultivaluedMap<String, ?> sanitizedHeaders = sanitizeHeaders(headers);

    assertThat(sanitizedHeaders.getFirst(AUTHORIZATION)).isEqualTo("Bearer ...");
  }
}
