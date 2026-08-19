package org.sdase.commons.client.jersey.filter;

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_ENCODING;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;

class CompressedResponseHeaderFilterTest {

  private static final String CONTENT_MD5 = "Content-MD5";

  private final CompressedResponseHeaderFilter filter = new CompressedResponseHeaderFilter();

  @Test
  void removeEntityTransportHeadersFromCompressedResponse() {
    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
    headers.add("content-encoding", "gzip");
    headers.add("CONTENT-length", "42");
    headers.add("content-md5", "checksum");
    headers.add("Content-Type", "application/json");

    filter.filter(null, responseContext(headers, "gzip"));

    assertThat(headers).containsOnlyKeys("Content-Type");
  }

  @Test
  void keepEntityTransportHeadersForUncompressedResponse() {
    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
    headers.add(CONTENT_ENCODING, "identity");
    headers.add(CONTENT_LENGTH, "42");
    headers.add(CONTENT_MD5, "checksum");

    filter.filter(null, responseContext(headers, "identity"));

    assertThat(headers).containsKeys(CONTENT_ENCODING, CONTENT_LENGTH, CONTENT_MD5);
  }

  private ClientResponseContext responseContext(
      MultivaluedMap<String, String> headers, String contentEncoding) {
    ClientResponseContext responseContext = mock(ClientResponseContext.class);
    when(responseContext.getHeaders()).thenReturn(headers);
    when(responseContext.getHeaderString(CONTENT_ENCODING)).thenReturn(contentEncoding);
    return responseContext;
  }
}
