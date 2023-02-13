package org.sdase.commons.server.dropwizard.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetadataContextClientRequestFilterTest {

  MetadataContextClientRequestFilter metadataContextClientRequestFilter =
      new MetadataContextClientRequestFilter(Set.of("tenant-id", "processes"));

  MultivaluedMap<String, Object> actualHeaders;
  ClientRequestContext givenClientRequestContext;

  @BeforeEach
  void setUp() {
    actualHeaders = new MultivaluedHashMap<>();
    givenClientRequestContext = mock(ClientRequestContext.class);
    when(givenClientRequestContext.getHeaders()).thenReturn(actualHeaders);

    MetadataContextHolder.clear();
  }

  @Test
  void shouldForwardAllConfiguredFields() throws IOException {
    givenCurrentContext(Map.of("tenant-id", List.of("t-1"), "processes", List.of("p-1")));

    metadataContextClientRequestFilter.filter(givenClientRequestContext);

    assertThat(actualHeaders)
        .contains(entry("tenant-id", List.of("t-1")), entry("processes", List.of("p-1")));
  }

  @Test
  void shouldForwardMultipleValues() throws IOException {
    givenCurrentContext(Map.of("tenant-id", List.of("t-1", "t-2")));

    metadataContextClientRequestFilter.filter(givenClientRequestContext);

    assertThat(actualHeaders).contains(entry("tenant-id", List.of("t-1", "t-2")));
  }

  @Test
  void shouldIgnoreNotConfiguredFields() throws IOException {
    givenCurrentContext(Map.of("brand", List.of("porsche")));

    metadataContextClientRequestFilter.filter(givenClientRequestContext);

    assertThat(actualHeaders).isEmpty();
  }

  @Test
  void shouldFilterEmptyValues() throws IOException {
    givenCurrentContext(Map.of("tenant-id", List.of("  ", "t-1", "t-2")));

    metadataContextClientRequestFilter.filter(givenClientRequestContext);

    assertThat(actualHeaders).contains(entry("tenant-id", List.of("t-1", "t-2")));
  }

  @Test
  void shouldNotAddAnythingWithoutConfiguration() throws IOException {
    var notConfiguredMetadataContextClientRequestFilter =
        new MetadataContextClientRequestFilter(Set.of());
    givenCurrentContext(Map.of("tenant-id", List.of("  ", "t-1", "t-2")));

    notConfiguredMetadataContextClientRequestFilter.filter(givenClientRequestContext);

    assertThat(actualHeaders).isEmpty();
  }

  private void givenCurrentContext(Map<String, List<String>> contextData) {
    var given = new DetachedMetadataContext();
    given.putAll(contextData);
    MetadataContext.createContext(given);
  }
}
