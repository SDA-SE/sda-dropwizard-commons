package org.sdase.commons.shared.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;

class TraceTokenContextTest {

  private static final String EXPECTED_TRACE_TOKEN_MDC_KEY = "Trace-Token";

  @BeforeEach
  @AfterEach
  void clearMdc() {
    MDC.remove("Trace-Token");
  }

  @Test
  void shouldCreateNewContext() {
    var actual = TraceTokenContext.getOrCreateTraceTokenContext();
    assertThat(actual.isCreated()).isTrue();
    assertThat(actual.isReused()).isFalse();
    assertThat(actual.get()).isNotBlank();

    assertThat(MDC.get(EXPECTED_TRACE_TOKEN_MDC_KEY)).isNotBlank().isEqualTo(actual.get());
  }

  @Test
  void shouldReuseContext() {

    var existingContext = TraceTokenContext.getOrCreateTraceTokenContext();

    var actual = TraceTokenContext.getOrCreateTraceTokenContext();

    assertThat(actual.isCreated()).isFalse();
    assertThat(actual.isReused()).isTrue();
    assertThat(actual.get()).isNotBlank();
    assertThat(MDC.get(EXPECTED_TRACE_TOKEN_MDC_KEY))
        .isNotBlank()
        .isEqualTo(actual.get())
        .isEqualTo(existingContext.get());
  }

  @Test
  void shouldCloseCreatedContext() {
    var actual = TraceTokenContext.getOrCreateTraceTokenContext();
    assertThat(MDC.get(EXPECTED_TRACE_TOKEN_MDC_KEY)).isNotBlank().isEqualTo(actual.get());
    actual.closeIfCreated();

    assertThat(MDC.get(EXPECTED_TRACE_TOKEN_MDC_KEY)).isNull();
  }

  @Test
  void shouldAutoCloseCreatedContext() {
    try (var actual = TraceTokenContext.getOrCreateTraceTokenContext()) {

      assertThat(actual.isCreated()).isTrue();
      assertThat(actual.isReused()).isFalse();
      assertThat(actual.get()).isNotBlank();
      assertThat(MDC.get(EXPECTED_TRACE_TOKEN_MDC_KEY)).isNotBlank().isEqualTo(actual.get());
    }
    assertThat(MDC.get(EXPECTED_TRACE_TOKEN_MDC_KEY)).isNull();
  }

  @Test
  void documentAutoClosableScope() {
    boolean passedCatch;
    boolean passedFinally;
    try (var actual = TraceTokenContext.getOrCreateTraceTokenContext()) {

      assertThat(actual.isCreated()).isTrue();
      assertThat(actual.isReused()).isFalse();
      assertThat(actual.get()).isNotBlank();
      assertThat(MDC.get(EXPECTED_TRACE_TOKEN_MDC_KEY)).isNotBlank().isEqualTo(actual.get());
      throw new UnsupportedOperationException();
    } catch (UnsupportedOperationException e) {
      passedCatch = true;
      assertThat(MDC.get(EXPECTED_TRACE_TOKEN_MDC_KEY)).isNull();
    } finally {
      passedFinally = true;
      assertThat(MDC.get(EXPECTED_TRACE_TOKEN_MDC_KEY)).isNull();
    }
    assertThat(passedCatch).isTrue();
    assertThat(passedFinally).isTrue();
  }

  @Test
  void shouldNotCloseReusedContext() {
    var existingContext = TraceTokenContext.getOrCreateTraceTokenContext();

    var actual = TraceTokenContext.getOrCreateTraceTokenContext();
    assertThat(MDC.get(EXPECTED_TRACE_TOKEN_MDC_KEY))
        .isNotBlank()
        .isEqualTo(actual.get())
        .isEqualTo(existingContext.get());
    actual.closeIfCreated();

    assertThat(MDC.get(EXPECTED_TRACE_TOKEN_MDC_KEY))
        .isNotBlank()
        .isEqualTo(actual.get())
        .isEqualTo(existingContext.get());
  }

  @Test
  void shouldNotAutoCloseReusedContext() {
    var existingContext = TraceTokenContext.getOrCreateTraceTokenContext();

    try (var actual = TraceTokenContext.getOrCreateTraceTokenContext()) {

      assertThat(actual.isCreated()).isFalse();
      assertThat(actual.isReused()).isTrue();
      assertThat(actual.get()).isNotBlank();
      assertThat(MDC.get(EXPECTED_TRACE_TOKEN_MDC_KEY))
          .isNotBlank()
          .isEqualTo(actual.get())
          .isEqualTo(existingContext.get());
    }
    assertThat(MDC.get(EXPECTED_TRACE_TOKEN_MDC_KEY)).isNotBlank().isEqualTo(existingContext.get());
  }

  @Test
  void shouldCreateRandomTraceTokens() {
    Set<String> createdTraceTokens = new HashSet<>();
    for (int i = 0; i < 100; i++) {
      try (var traceTokenContext = TraceTokenContext.getOrCreateTraceTokenContext()) {
        createdTraceTokens.add(traceTokenContext.get());
      }
    }
    assertThat(createdTraceTokens).hasSize(100);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void shouldCreateNewContextIfIncomingIsBlank(String incomingTraceToken) {
    try (var actual = TraceTokenContext.continueSynchronousTraceTokenContext(incomingTraceToken)) {
      assertThat(actual.isCreated()).isTrue();
      assertThat(actual.isReused()).isFalse();
      assertThat(actual.get()).isNotBlank();
      assertThat(MDC.get(EXPECTED_TRACE_TOKEN_MDC_KEY)).isNotBlank().isEqualTo(actual.get());
    }
  }

  @Test
  void shouldCreateNewContextIfIncomingIsNull() {
    try (var actual = TraceTokenContext.continueSynchronousTraceTokenContext(null)) {
      assertThat(actual.isCreated()).isTrue();
      assertThat(actual.isReused()).isFalse();
      assertThat(actual.get()).isNotBlank();
      assertThat(MDC.get(EXPECTED_TRACE_TOKEN_MDC_KEY)).isNotBlank().isEqualTo(actual.get());
    }
  }

  @Test
  void shouldKeepIncomingTraceToken() {
    var incomingTraceToken = "incoming-trace-token";
    try (var actual = TraceTokenContext.continueSynchronousTraceTokenContext(incomingTraceToken)) {
      assertThat(actual.isCreated()).isTrue();
      assertThat(actual.isReused()).isFalse();
      assertThat(actual.get()).isNotBlank();
      assertThat(MDC.get(EXPECTED_TRACE_TOKEN_MDC_KEY))
          .isNotBlank()
          .isEqualTo(actual.get())
          .isEqualTo(incomingTraceToken);
    }
  }
}
