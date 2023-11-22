package org.sdase.commons.server.dropwizard.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetadataContextFilterTest {

  @BeforeEach
  void clear() {
    MetadataContextHolder.clear();
  }

  @Test
  void shouldStoreOnlyMetadataContextFieldsInRequest() throws IOException {
    var metadataContextFilter = new MetadataContextFilter(Set.of("tenant-id", "processes"));
    var headers = new MultivaluedStringMap();
    headers.put("tenant-id", List.of("t1"));
    headers.put("processes", List.of("p1", "p2"));
    headers.put("other-header", List.of("hello"));
    metadataContextFilter.filter(requestWithHeaders(headers));

    assertThat(MetadataContext.current().keys())
        .containsExactlyInAnyOrder("tenant-id", "processes");
    assertThat(MetadataContext.current().valuesByKey("tenant-id")).containsExactly("t1");
    assertThat(MetadataContext.current().valuesByKey("processes")).containsExactly("p1", "p2");
  }

  @Test
  void shouldSplitFromSingleValue() throws IOException {
    var metadataContextFilter = new MetadataContextFilter(Set.of("tenant-id"));
    var headers = new MultivaluedStringMap();
    headers.put("tenant-id", List.of("t1,t2, t3 , t4"));
    metadataContextFilter.filter(requestWithHeaders(headers));

    assertThat(MetadataContext.current().keys()).containsExactlyInAnyOrder("tenant-id");
    assertThat(MetadataContext.current().valuesByKey("tenant-id"))
        .containsExactly("t1", "t2", "t3", "t4");
  }

  @Test
  void shouldSplitFromMultipleValues() throws IOException {
    var metadataContextFilter = new MetadataContextFilter(Set.of("tenant-id"));
    var headers = new MultivaluedStringMap();
    headers.put("tenant-id", List.of("t1,t2", " t3 , t4"));
    metadataContextFilter.filter(requestWithHeaders(headers));

    assertThat(MetadataContext.current().keys()).containsExactlyInAnyOrder("tenant-id");
    assertThat(MetadataContext.current().valuesByKey("tenant-id"))
        .containsExactly("t1", "t2", "t3", "t4");
  }

  @Test
  void shouldStoreIncompleteMetadataContextFieldsInRequest() throws IOException {
    var metadataContextFilter = new MetadataContextFilter(Set.of("tenant-id", "processes"));
    var headers = new MultivaluedStringMap();
    headers.put("tenant-id", List.of("t1"));
    headers.put("other-header", List.of("hello"));
    metadataContextFilter.filter(requestWithHeaders(headers));

    assertThat(MetadataContext.current().keys())
        .containsExactlyInAnyOrder("tenant-id", "processes");
    assertThat(MetadataContext.current().valuesByKey("tenant-id")).containsExactly("t1");
    assertThat(MetadataContext.current().valuesByKey("processes")).isEmpty();
  }

  @Test
  void shouldClearAfterRequest() throws IOException {
    var metadataContextFilter = new MetadataContextFilter(Set.of("tenant-id", "processes"));
    var before = new DetachedMetadataContext();
    before.put("tenant-id", List.of("t1"));
    MetadataContext.createContext(before);

    // precondition
    assertThat(MetadataContext.current().keys()).containsExactly("tenant-id");

    metadataContextFilter.filter(null, null);

    assertThat(MetadataContext.current().keys()).isEmpty();
  }

  ContainerRequestContext requestWithHeaders(MultivaluedMap<String, String> headers) {
    ContainerRequestContext mock = mock(ContainerRequestContext.class);
    when(mock.getHeaders()).thenReturn(headers);
    return mock;
  }
}
