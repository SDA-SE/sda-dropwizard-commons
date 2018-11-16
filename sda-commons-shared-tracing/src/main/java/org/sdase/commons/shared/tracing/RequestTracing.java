package org.sdase.commons.shared.tracing;

/**
 * Shared properties to trace all service calls initiated by a request entering SDA platform.
 * The trace token is generated when not existing and shared in the request and used for logging and monitoring.
 */
public class RequestTracing {

   private RequestTracing() {
   }

   /**
    * The header name used to send the request token.
    */
   public static final String TOKEN_HEADER = "Trace-Token";

   /**
    * Common name to share the token internally, e.g. as attribute in the request context
    */
   public static final String TOKEN_ATTRIBUTE = RequestTracing.class.getName() + "_TOKEN";

   /**
    * Name of the trace token used to promote it in the {@code org.slf4j.MDC}
    */
   public static final String TOKEN_MDC_KEY = "Trace-Token";

}
