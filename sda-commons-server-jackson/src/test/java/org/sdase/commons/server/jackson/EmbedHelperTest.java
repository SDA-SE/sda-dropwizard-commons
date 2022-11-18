package org.sdase.commons.server.jackson;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.dropwizard.setup.Environment;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Test;

class EmbedHelperTest {
  @Test
  void shouldEmbedResourceIfRequested() {
    MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();
    queryParameters.put("embed", singletonList("drivers"));
    EmbedHelper embedHelper = createEmbedHelper(queryParameters);

    assertThat(embedHelper.isEmbeddingOfRelationRequested("drivers")).isTrue();
    assertThat(embedHelper.isEmbeddingOfRelationRequested("owner")).isFalse();
  }

  @Test
  void shouldEmbedMultipleResourcesIfRequested() {
    MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();
    queryParameters.put("embed", asList("drivers", "owner"));
    EmbedHelper embedHelper = createEmbedHelper(queryParameters);

    assertThat(embedHelper.isEmbeddingOfRelationRequested("drivers")).isTrue();
    assertThat(embedHelper.isEmbeddingOfRelationRequested("owner")).isTrue();
  }

  private EmbedHelper createEmbedHelper(MultivaluedMap<String, String> queryParameters) {
    Environment environment = mock(Environment.class, RETURNS_DEEP_STUBS);
    EmbedHelper embedHelper = new EmbedHelper(environment);
    embedHelper.uriInfo = mock(UriInfo.class);
    when(embedHelper.uriInfo.getQueryParameters()).thenReturn(queryParameters);
    return embedHelper;
  }
}
