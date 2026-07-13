package org.sdase.commons.server.trace.filter;

import jakarta.ws.rs.container.PreMatching;

/**
 * A request filter land response filter that detects, optionally generates if not existing and
 * provides the trace token in requests.
 *
 * @deprecated This class will be replaced by {@link
 *     org.sdase.commons.server.dropwizard.filter.TraceTokenServerFilter} when removing the module
 *     {@code sda-commons-server-trace}. To prepare for the upcoming breaking change, update all
 *     references to {@link org.sdase.commons.server.dropwizard.filter.TraceTokenServerFilter} and
 *     remove direct dependencies to {@code sda-commons-server-trace}.
 */
@Deprecated(forRemoval = true)
@PreMatching // No matching is required, should happen as early as possible
@SuppressWarnings("java:S2176") // intentionally the same name until removed
public class TraceTokenServerFilter
    extends org.sdase.commons.server.dropwizard.filter.TraceTokenServerFilter {}
