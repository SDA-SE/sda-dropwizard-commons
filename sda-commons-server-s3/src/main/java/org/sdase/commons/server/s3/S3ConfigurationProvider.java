package org.sdase.commons.server.s3;

import java.util.function.Function;

import io.dropwizard.Configuration;

/**
 * Provides the {@link S3Configuration} for the {@link S3Bundle}, e.g.
 * {@code MyAppConfig::getS3Config}
 *
 * @param <C>
 *           the type of the applications configuration class
 */
@FunctionalInterface
public interface S3ConfigurationProvider<C extends Configuration> extends Function<C, S3Configuration> {
}
