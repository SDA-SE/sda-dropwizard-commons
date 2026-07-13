package org.sdase.commons.server.cors;

/**
 * @deprecated This class will be replaced by {@link
 *     org.sdase.commons.server.dropwizard.bundles.CorsConfiguration} when removing the module
 *     {@code sda-commons-server-cors}. To prepare for the upcoming breaking change, update all
 *     references to {@link org.sdase.commons.server.dropwizard.bundles.CorsConfiguration} and
 *     remove direct dependencies to {@code sda-commons-server-cors}.
 */
@Deprecated(forRemoval = true)
@SuppressWarnings("java:S2176") // intentionally the same name until removed
public class CorsConfiguration
    extends org.sdase.commons.server.dropwizard.bundles.CorsConfiguration {}
