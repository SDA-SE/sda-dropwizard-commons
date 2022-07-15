package org.sdase.commons.server.consumer.filter;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sdase.commons.shared.api.error.ApiException;
import org.sdase.commons.shared.tracing.ConsumerTracing;

class ConsumerTokenServerFilterTest {

  private ContainerRequestContext requestContext;
  private UriInfo uriInfo;

  @BeforeEach
  void setUp() {
    uriInfo = mock(UriInfo.class);
    requestContext = mock(ContainerRequestContext.class);
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
  }

  @Test
  void shouldThrowApiExceptionIfConsumerTokenIsRequiredButNotPresent() {
    // given
    when(uriInfo.getPath()).thenReturn("/api/entities");

    // when
    ConsumerTokenServerFilter filter = new ConsumerTokenServerFilter(true, asList("foo", "bar"));

    // then
    assertThatCode(() -> filter.filter(requestContext)).isInstanceOf(ApiException.class);
  }

  @Test
  void shouldNotThrowApiExceptionIfPathIsExcluded() {
    // given
    when(uriInfo.getPath()).thenReturn("/api/entities");

    // when
    ConsumerTokenServerFilter filter =
        new ConsumerTokenServerFilter(true, singletonList("\\/api\\/.*"));

    // then
    assertThatCode(() -> filter.filter(requestContext)).doesNotThrowAnyException();
  }

  @Test
  void shouldNotThrowApiExceptionIfPathConsumerTokenIsPresent() {
    // given
    when(uriInfo.getPath()).thenReturn("/api/entities");
    when(requestContext.getHeaderString(ConsumerTracing.TOKEN_HEADER)).thenReturn("my-consumer");

    // when
    ConsumerTokenServerFilter filter = new ConsumerTokenServerFilter(true, asList("foo", "bar"));

    // then
    assertThatCode(() -> filter.filter(requestContext)).doesNotThrowAnyException();
  }

  @Test
  void shouldNotThrowApiExceptionIfConsumerTokenIsNotRequired() {
    // given
    when(uriInfo.getPath()).thenReturn("/api/entities");

    // when
    ConsumerTokenServerFilter filter = new ConsumerTokenServerFilter(false, singletonList("foo"));

    // then
    assertThatCode(() -> filter.filter(requestContext)).doesNotThrowAnyException();
  }

  @Test
  void shouldBeEqualIfSameInstance() {
    ConsumerTokenServerFilter filter = new ConsumerTokenServerFilter(false, singletonList("foo"));
    assertThat(filter).isEqualTo(filter);
  }

  @Test
  void shouldBeEqualIfSameFields() {
    ConsumerTokenServerFilter filter1 = new ConsumerTokenServerFilter(false, singletonList("foo"));
    ConsumerTokenServerFilter filter2 = new ConsumerTokenServerFilter(false, singletonList("foo"));
    assertThat(filter1).isEqualTo(filter2);
  }

  @Test
  void shouldNotBeEqualIfDifferentExcludeRegex() {
    ConsumerTokenServerFilter filter1 = new ConsumerTokenServerFilter(false, singletonList("foo"));
    ConsumerTokenServerFilter filter2 = new ConsumerTokenServerFilter(false, singletonList("bar"));
    assertThat(filter1).isNotEqualTo(filter2);
  }

  @Test
  void shouldNotBeEqualIfDifferentRequireIdentifiedConsumer() {
    ConsumerTokenServerFilter filter1 = new ConsumerTokenServerFilter(false, singletonList("foo"));
    ConsumerTokenServerFilter filter2 = new ConsumerTokenServerFilter(true, singletonList("foo"));
    assertThat(filter1).isNotEqualTo(filter2);
  }
}
