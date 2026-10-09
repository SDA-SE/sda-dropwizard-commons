package org.sdase.commons.server.healthcheck.helper;

/**
 * @deprecated This class will be replaced by {@link
 *     org.sdase.commons.server.dropwizard.healthcheck.helper.OpenConnectionFunction} when removing
 *     the module {@code sda-commons-server-healthcheck}. To prepare for the upcoming breaking
 *     change, update all references to {@link
 *     org.sdase.commons.server.dropwizard.healthcheck.helper.OpenConnectionFunction} and remove
 *     direct dependencies to {@code sda-commons-server-healthcheck}.
 */
@Deprecated(forRemoval = true)
@SuppressWarnings("java:S2176") // intentionally the same name until removed
public interface OpenConnectionFunction
    extends org.sdase.commons.server.dropwizard.healthcheck.helper.OpenConnectionFunction {}
