package org.sdase.commons.server.healthcheck.servlet;

import org.sdase.commons.server.healthcheck.ExternalHealthCheck;

/**
 * filter that removes all health checks that implements the marker interface {@link
 * ExternalHealthCheck}
 *
 * @deprecated This class will be replaced by {@link
 *     org.sdase.commons.server.dropwizard.healthcheck.servlet.OnlyInternalHealthCheckFilter} when
 *     removing the module {@code sda-commons-server-healthcheck}. To prepare for the upcoming
 *     breaking change, update all references to {@link
 *     org.sdase.commons.server.dropwizard.healthcheck.servlet.OnlyInternalHealthCheckFilter} and
 *     remove direct dependencies to {@code sda-commons-server-healthcheck}.
 */
@Deprecated(forRemoval = true)
@SuppressWarnings("java:S2176") // intentionally the same name until removed
public class OnlyInternalHealthCheckFilter
    extends org.sdase.commons.server.dropwizard.healthcheck.servlet.OnlyInternalHealthCheckFilter {}
