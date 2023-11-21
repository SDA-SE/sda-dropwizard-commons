package org.sdase.commons.server.opentelemetry.client;

import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.core.setup.Environment;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.ContextPropagators;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TracedHttpClientInitialBuilderTest {
  @Test
  void shouldUseGivenOtel() {
    var givenPropagators = Mockito.mock(ContextPropagators.class, Mockito.RETURNS_DEEP_STUBS);
    var given = Mockito.mock(OpenTelemetry.class, Mockito.RETURNS_DEEP_STUBS);
    Mockito.when(given.getPropagators()).thenReturn(givenPropagators);
    @SuppressWarnings("removal")
    var actual =
        new TracedHttpClientInitialBuilder(
                Mockito.mock(Environment.class, Mockito.RETURNS_DEEP_STUBS))
            .usingTelemetryInstance(given)
            .build("otel"); // TODO verify if this is the correct approach
    assertThat(actual).extracting("propagators").isSameAs(givenPropagators);
  }
}
