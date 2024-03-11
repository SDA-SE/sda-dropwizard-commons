package org.sdase.commons.shared.tracing;

import java.util.UUID;
import org.slf4j.MDC;

/**
 * Manages the context for the {@code Trace-Token}. The {@code Trace-Token} is used to correlate log
 * messages across services for a single synchronous process. It will be logged as {@link
 * org.slf4j.MDC} key {@value #TRACE_TOKEN_MDC_KEY}.
 */
public class TraceTokenContext implements AutoCloseable {

  /** The key of the trace token in the {@link org.slf4j.MDC}. */
  // not private yet for RequestTracing
  static final String TRACE_TOKEN_MDC_KEY = "Trace-Token";

  /** The name of the header containing the trace token in HTTP requests. */
  public static final String TRACE_TOKEN_HTTP_HEADER_NAME = TRACE_TOKEN_MDC_KEY;

  private static final Object CREATE_SEMAPHORE = new Object();

  private final boolean created;
  private final String traceToken;

  private TraceTokenContext(boolean created, String traceToken) {
    this.created = created;
    this.traceToken = traceToken;
  }

  /**
   * @return if the {@value #TRACE_TOKEN_MDC_KEY} of this {@link TraceTokenContext} has been created
   *     for the caller of {@link #getOrCreateTraceTokenContext()}.
   */
  public boolean isCreated() {
    return created;
  }

  /**
   * @return if the {@value #TRACE_TOKEN_MDC_KEY} of this {@link TraceTokenContext} has been reused
   *     from an existing {@link TraceTokenContext} for the caller of {@link
   *     #getOrCreateTraceTokenContext()}.
   */
  public boolean isReused() {
    return !isCreated();
  }

  /**
   * @return the {@value TRACE_TOKEN_MDC_KEY} of this context.
   */
  public String get() {
    return traceToken;
  }

  /**
   * Closes this {@link TraceTokenContext} and removes all associated properties from the {@link
   * MDC} <strong>only if this {@link TraceTokenContext} represents the initial creation of the
   * context</strong>.
   */
  public void closeIfCreated() {
    if (this.created) {
      closeTraceTokenContext();
    }
  }

  /**
   * Closes this {@link TraceTokenContext} and removes all associated properties from the {@link
   * MDC} <strong>only if this {@link TraceTokenContext} represents the initial creation of the
   * context</strong>.
   */
  @Override
  public void close() {
    closeIfCreated();
  }

  /**
   * @return a {@link TraceTokenContext} either from the existing {@value TRACE_TOKEN_MDC_KEY} or a
   *     new random {@value TRACE_TOKEN_MDC_KEY}. All received instances must be {@linkplain
   *     #closeIfCreated() closed}. Only the context that started this trace will actually remove
   *     the {@value TRACE_TOKEN_MDC_KEY} from the {@link MDC}.
   */
  public static TraceTokenContext getOrCreateTraceTokenContext() {
    synchronized (CREATE_SEMAPHORE) {
      if (isTraceTokenContextActive()) {
        return new TraceTokenContext(false, MDC.get(TRACE_TOKEN_MDC_KEY));
      }
      MDC.put(TRACE_TOKEN_MDC_KEY, createTraceToken());
      return new TraceTokenContext(true, MDC.get(TRACE_TOKEN_MDC_KEY));
    }
  }

  /**
   * Continues the {@value TRACE_TOKEN_MDC_KEY} context of a synchronous process that started
   * outside this service. Must not be used for asynchronous processes!
   *
   * @param incomingTraceToken the {@value TRACE_TOKEN_MDC_KEY} received from an external
   *     synchronous process, e.g. via HTTP header {@value TRACE_TOKEN_HTTP_HEADER_NAME} of an
   *     incoming HTTP request. May be {@code null}, if no {@value TRACE_TOKEN_MDC_KEY} context was
   *     received.
   * @return a {@link TraceTokenContext} either from the given {@code incomingTraceToken} or a new
   *     random {@value TRACE_TOKEN_MDC_KEY}. All received instances must be {@linkplain
   *     #closeIfCreated() closed}. Only the context that started this trace will actually remove
   *     the {@value TRACE_TOKEN_MDC_KEY} from the {@link MDC}. In any case, this context will be
   *     considered as created and {@link #closeIfCreated()} and {@link #close()} will end the
   *     context.
   */
  public static TraceTokenContext continueSynchronousTraceTokenContext(String incomingTraceToken) {
    synchronized (CREATE_SEMAPHORE) {
      closeTraceTokenContext();
      if (incomingTraceToken != null && !incomingTraceToken.isBlank()) {
        MDC.put(TRACE_TOKEN_MDC_KEY, incomingTraceToken);
      } else {
        MDC.put(TRACE_TOKEN_MDC_KEY, createTraceToken());
      }
      return new TraceTokenContext(true, MDC.get(TRACE_TOKEN_MDC_KEY));
    }
  }

  /**
   * @return if there is an active {@value TRACE_TOKEN_MDC_KEY} context in the current Thread.
   */
  public static boolean isTraceTokenContextActive() {
    return MDC.get(TRACE_TOKEN_MDC_KEY) != null;
  }

  private static void closeTraceTokenContext() {
    MDC.remove(TRACE_TOKEN_MDC_KEY);
  }

  private static String createTraceToken() {
    return UUID.randomUUID().toString();
  }
}
