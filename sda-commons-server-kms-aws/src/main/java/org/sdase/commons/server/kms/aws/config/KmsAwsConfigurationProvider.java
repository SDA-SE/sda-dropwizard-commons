package org.sdase.commons.server.kms.aws.config;

import io.dropwizard.Configuration;
import java.util.function.Function;
import org.sdase.commons.server.kms.aws.KmsAwsBundle;

/**
 * Provides the {@link KmsAwsConfiguration} for the {@link KmsAwsBundle}, e.g. {@code
 * MyAppConfig::getSqsConfig}
 *
 * @param <C> the type of the applications configuration class
 */
@FunctionalInterface
public interface KmsAwsConfigurationProvider<C extends Configuration>
    extends Function<C, KmsAwsConfiguration> {}
