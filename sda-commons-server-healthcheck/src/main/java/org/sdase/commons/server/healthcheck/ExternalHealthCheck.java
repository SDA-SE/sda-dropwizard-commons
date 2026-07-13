package org.sdase.commons.server.healthcheck;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker to identify if an HealthCheck is used for external Checks which are outside of the
 * microservice context
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
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@org.sdase.commons.server.dropwizard.healthcheck.ExternalHealthCheck
public @interface ExternalHealthCheck {}
