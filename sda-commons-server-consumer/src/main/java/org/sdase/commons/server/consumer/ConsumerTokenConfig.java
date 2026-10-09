package org.sdase.commons.server.consumer;

/**
 * @deprecated This class will be replaced by {@link
 *     org.sdase.commons.server.dropwizard.bundles.ConsumerTokenConfig} when removing the module
 *     {@code sda-commons-server-consumer}. To prepare for the upcoming breaking change, update all
 *     references to {@link org.sdase.commons.server.dropwizard.bundles.ConsumerTokenConfig} and
 *     remove direct dependencies to {@code sda-commons-server-consumer}.
 */
@Deprecated(forRemoval = true)
@SuppressWarnings("java:S2176") // intentionally the same name until removed
public class ConsumerTokenConfig
    extends org.sdase.commons.server.dropwizard.bundles.ConsumerTokenConfig {}
