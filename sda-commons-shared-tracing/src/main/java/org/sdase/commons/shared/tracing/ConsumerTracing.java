package org.sdase.commons.shared.tracing;

/**
 * Shared properties to trace the consumer that initiated a request. A consumer identifies itself in
 * the request header. The consumer information is shared in the request and used for logging and
 * monitoring.
 *
 * @deprecated This class will be replaced by {@link
 *     org.sdase.commons.server.dropwizard.tracing.ConsumerTracing} when removing the module {@code
 *     sda-commons-shared-tracing}. To prepare for the upcoming breaking change, update all
 *     references to {@link org.sdase.commons.server.dropwizard.tracing.ConsumerTracing} and remove
 *     direct dependencies to {@code sda-commons-shared-tracing}.
 */
@Deprecated(forRemoval = true)
@SuppressWarnings("java:S2176") // intentionally the same name until removed
public class ConsumerTracing extends org.sdase.commons.server.dropwizard.tracing.ConsumerTracing {

  private ConsumerTracing() {}
}
