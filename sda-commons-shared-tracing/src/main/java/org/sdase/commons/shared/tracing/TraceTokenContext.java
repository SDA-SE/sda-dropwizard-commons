package org.sdase.commons.shared.tracing;

import org.slf4j.MDC;

/**
 * Manages the context for the {@code Trace-Token}. The {@code Trace-Token} is used to correlate log
 * messages across services for a single synchronous process. It will be logged as {@link
 * org.slf4j.MDC} key {@code Trace-Token}.
 *
 * @deprecated This class will be replaced by {@link
 *     org.sdase.commons.server.dropwizard.tracing.TraceTokenContext} when removing the module
 *     {@code sda-commons-shared-tracing}. To prepare for the upcoming breaking change, update all
 *     references to {@link org.sdase.commons.server.dropwizard.tracing.TraceTokenContext} and
 *     remove direct dependencies to {@code sda-commons-shared-tracing}.
 */
@Deprecated(forRemoval = true)
@SuppressWarnings("java:S2176") // intentionally the same name until removed
public class TraceTokenContext
    extends org.sdase.commons.server.dropwizard.tracing.TraceTokenContext {

  private TraceTokenContext(
      org.sdase.commons.server.dropwizard.tracing.TraceTokenContext traceTokenContext) {
    super(traceTokenContext.isCreated(), traceTokenContext.get());
  }

  /**
   * @return a {@link TraceTokenContext} either from the existing {@code Trace-Token} or a new
   *     random {@code Trace-Token}. All received instances must be {@linkplain #closeIfCreated()
   *     closed}. Only the context that started this trace will actually remove the {@code
   *     Trace-Token} from the {@link MDC}.
   */
  public static TraceTokenContext getOrCreateTraceTokenContext() {
    return new TraceTokenContext(
        org.sdase.commons.server.dropwizard.tracing.TraceTokenContext
            .getOrCreateTraceTokenContext());
  }

  /**
   * Continues the {@code Trace-Token} context of a synchronous process that started outside this
   * service. Must not be used for asynchronous processes!
   *
   * @param incomingTraceToken the {@code Trace-Token} received from an external synchronous
   *     process, e.g. via HTTP header {@value TRACE_TOKEN_HTTP_HEADER_NAME} of an incoming HTTP
   *     request. May be {@code null}, if no {@code Trace-Token} context was received.
   * @return a {@link TraceTokenContext} either from the given {@code incomingTraceToken} or a new
   *     random {@code Trace-Token}. All received instances must be {@linkplain #closeIfCreated()
   *     closed}. Only the context that started this trace will actually remove the {@code
   *     Trace-Token} from the {@link MDC}. In any case, this context will be considered as created
   *     and {@link #closeIfCreated()} and {@link #close()} will end the context.
   */
  public static TraceTokenContext continueSynchronousTraceTokenContext(String incomingTraceToken) {
    return new TraceTokenContext(
        org.sdase.commons.server.dropwizard.tracing.TraceTokenContext
            .continueSynchronousTraceTokenContext(incomingTraceToken));
  }

  /**
   * Creates a new {@link TraceTokenContext} with a reference to a parent context. This should be
   * used to keep track of asynchronous processes to have a connection from the producer process to
   * the new consumer process(es).
   *
   * @param incomingParentTraceToken the {@code Parent-Trace-Token} received from an external
   *     asynchronous process, e.g. via message header {@value TRACE_TOKEN_MESSAGING_HEADER_NAME} of
   *     a consumed Kafka message. May be {@code null}, if no {@value
   *     TRACE_TOKEN_MESSAGING_HEADER_NAME} context was received.
   * @return a {@link TraceTokenContext} with a reference to the given {@code
   *     incomingParentTraceToken} as {@code Parent-Trace-Token} in the {@link MDC}. If there is a
   *     current trace token context that already has a {@code Parent-Trace-Token}, the current
   *     context is not changed. All received instances must be {@linkplain #closeIfCreated()
   *     closed}. Only the context that started this trace will actually remove the {@code
   *     Trace-Token} from the {@link MDC}.
   */
  public static org.sdase.commons.server.dropwizard.tracing.TraceTokenContext
      createFromAsynchronousTraceTokenContext(String incomingParentTraceToken) {
    return new TraceTokenContext(
        org.sdase.commons.server.dropwizard.tracing.TraceTokenContext
            .createFromAsynchronousTraceTokenContext(incomingParentTraceToken));
  }
}
