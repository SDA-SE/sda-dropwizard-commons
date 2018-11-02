package com.sdase.commons.server.kafka;

import io.dropwizard.Configuration;

import java.util.function.Function;

/**
 * Provides the configuration for the kafka bundle {@link KafkaConfiguration}, e.g. {@code MyAppConfig::getKafka}
 *
 * @param <C> the type of the applications configuration class
 */
@FunctionalInterface
public interface KafkaConfigurationProvider<C extends Configuration> extends Function<C, KafkaConfiguration> {
}
