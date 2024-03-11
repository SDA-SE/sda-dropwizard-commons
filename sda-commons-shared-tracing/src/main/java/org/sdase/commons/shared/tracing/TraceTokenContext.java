package org.sdase.commons.shared.tracing;

import java.io.Closeable;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Manages the context for the {@code Trace-Token}. The {@code Trace-Token} is used to correlate log
 * messages across services for a single synchronous process. It will be logged as {@link
 * org.slf4j.MDC} key {@value #TRACE_TOKEN_MDC_KEY}.
 */
public class TraceTokenContext implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(TraceTokenContext.class);

  /** The key of the trace token in the {@link org.slf4j.MDC}. */
  // not private yet for RequestTracing
  static final String TRACE_TOKEN_MDC_KEY = "Trace-Token";

  /**
   * The name used to put an incoming {@value TRACE_TOKEN_MDC_KEY} in the {@link MDC} which logging
   * trace should not be continued but related to the logs of the current synchronous process. The
   * key is used, when a new {@value TRACE_TOKEN_MDC_KEY} is created when receiving {@value
   * TRACE_TOKEN_MDC_KEY} information from an asynchronous process.
   */
  private static final String PARENT_TRACE_TOKEN_MDC_KEY = "Parent-Trace-Token";

  /** The name of the header containing the trace token in HTTP requests. */
  public static final String TRACE_TOKEN_HTTP_HEADER_NAME = TRACE_TOKEN_MDC_KEY;

  /**
   * The name of the header containing the trace token in asynchronous messages. Note that the trace
   * token context should not be continued when processing asynchronous messages. It should only be
   * used to connect a new trace token with the received trace token from the producer.
   */
  public static final String TRACE_TOKEN_MESSAGING_HEADER_NAME = "Parent-Trace-Token";

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
        return new TraceTokenContext(false, currentTraceToken());
      }
      saveTraceToken(createTraceToken());
      return new TraceTokenContext(true, currentTraceToken());
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
        saveTraceToken(incomingTraceToken);
      } else {
        saveTraceToken(createTraceToken());
      }
      return new TraceTokenContext(true, currentTraceToken());
    }
  }

  /**
   * Creates a new {@link TraceTokenContext} with a reference to a parent context. This should be
   * used to keep track of asynchronous processes to have a connection from the producer process to
   * the new consumer process(es).
   *
   * @param incomingParentTraceToken the {@value PARENT_TRACE_TOKEN_MDC_KEY} received from an
   *     external asynchronous process, e.g. via message header {@value
   *     TRACE_TOKEN_MESSAGING_HEADER_NAME} of a consumed Kafka message. May be {@code null}, if no
   *     {@value TRACE_TOKEN_MESSAGING_HEADER_NAME} context was received.
   * @return a {@link TraceTokenContext} with a reference to the given {@code
   *     incomingParentTraceToken} as {@value PARENT_TRACE_TOKEN_MDC_KEY} in the {@link MDC}. If
   *     there is a current trace token context that already has a {@value
   *     PARENT_TRACE_TOKEN_MDC_KEY}, the current context is not changed. All received instances
   *     must be {@linkplain #closeIfCreated() closed}. Only the context that started this trace
   *     will actually remove the {@value TRACE_TOKEN_MDC_KEY} from the {@link MDC}.
   */
  public static TraceTokenContext createFromAsynchronousTraceTokenContext(
      String incomingParentTraceToken) {
    synchronized (CREATE_SEMAPHORE) {
      if (isTraceContextActiveWithParent()) {
        LOG.info(
            "Not adding parent trace token '{}' to existing trace token context with parent.",
            incomingParentTraceToken);
        return new TraceTokenContext(false, currentTraceToken());
      }

      if (incomingParentTraceToken != null && !incomingParentTraceToken.isBlank()) {
        MDC.put(PARENT_TRACE_TOKEN_MDC_KEY, incomingParentTraceToken);
      }

      if (isTraceTokenContextActive()) {
        return new TraceTokenContext(false, currentTraceToken());
      }

      saveTraceToken(createTraceToken());
      return new TraceTokenContext(true, currentTraceToken());
    }
  }

  /**
   * @return if there is an active {@value TRACE_TOKEN_MDC_KEY} context in the current Thread.
   */
  public static boolean isTraceTokenContextActive() {
    return currentTraceToken() != null;
  }

  private static boolean isTraceContextActiveWithParent() {
    return currentTraceToken() != null && MDC.get(PARENT_TRACE_TOKEN_MDC_KEY) != null;
  }

  private static void closeTraceTokenContext() {
    MDC.remove(TRACE_TOKEN_MDC_KEY);
    MDC.remove(PARENT_TRACE_TOKEN_MDC_KEY);
  }

  private static String createTraceToken() {
    return UUID.randomUUID().toString();
  }

  private static String currentTraceToken() {
    return MDC.get(TRACE_TOKEN_MDC_KEY);
  }

  private static void saveTraceToken(String traceToken) {
    MDC.put(TRACE_TOKEN_MDC_KEY, traceToken);
  }
}
