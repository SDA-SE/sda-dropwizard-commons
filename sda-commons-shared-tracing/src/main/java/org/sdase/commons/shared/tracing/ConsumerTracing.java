package org.sdase.commons.shared.tracing;

/**
 * Shared properties to trace the consumer that initiated a request. A consumer identifies itself in the request header.
 * The consumer information is shared in the request and used for logging and monitoring.
 */
public class ConsumerTracing {

   private ConsumerTracing() {
   }

   /**
    * The header name used to send the consumer token.
    */
   public static final String TOKEN_HEADER = "Consumer-Token";

   /**
    * Common name to share the consumer name internally, e.g. as attribute in the request context
    */
   public static final String NAME_ATTRIBUTE = ConsumerTracing.class.getName() + "_NAME";

   /**
    * Common name to share the consumer token internally, e.g. as attribute in the request context
    */
   public static final String TOKEN_ATTRIBUTE = ConsumerTracing.class.getName() + "_TOKEN";

   /**
    * Name of the consumer name used to promote it in the {@code org.slf4j.MDC}
    */
   public static final String NAME_MDC_KEY = "Consumer-Name";

}
