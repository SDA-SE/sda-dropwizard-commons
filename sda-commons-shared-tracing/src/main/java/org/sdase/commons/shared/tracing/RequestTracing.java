package org.sdase.commons.shared.tracing;

/**
 * Shared properties to trace all service calls initiated by a request entering SDA platform. The
 * trace token is generated when not existing and shared in the request and used for logging and
 * monitoring. Note that the purpose is different from the Trace ID of Open Telemetry which is used
 * to find.
 *
 * @deprecated all kept properties defined here moved to {@link TraceTokenContext}. Trace-Tokens
 *     should be obtained with {@link TraceTokenContext#getOrCreateTraceTokenContext()}. On all
 *     obtained contexts {@link TraceTokenContext#closeIfCreated()} or {@link AutoCloseable#close()}
 *     must be called after the local context finished.
 */
@Deprecated(forRemoval = true)
public class RequestTracing {

  private RequestTracing() {}

  /**
   * The header name used to send the request token.
   *
   * @deprecated in favor of {@link TraceTokenContext#TRACE_TOKEN_HTTP_HEADER_NAME}
   */
  @Deprecated(forRemoval = true)
  public static final String TOKEN_HEADER = "Trace-Token";

  /**
   * Common name to share the token internally, e.g. as attribute in the request context
   *
   * @deprecated As {@value TOKEN_HEADER} support is no longer a unique feature for HTTP requests,
   *     the primary storage for the trace token of the current context is the MDC. In contrast to
   *     Open Telemetry's trace id, the {@value TOKEN_HEADER} is used to correlate log message.
   *     Therefore, the {@link org.slf4j.MDC} is used to save the context.
   * @see TraceTokenContext
   */
  @Deprecated(forRemoval = true)
  public static final String TOKEN_ATTRIBUTE = RequestTracing.class.getName() + "_TOKEN";

  /**
   * Name of the trace token used to promote it in the {@code org.slf4j.MDC}
   *
   * @deprecated in favor of {@link TraceTokenContext}
   */
  @Deprecated(forRemoval = true)
  public static final String TOKEN_MDC_KEY = TraceTokenContext.TRACE_TOKEN_MDC_KEY;
}
