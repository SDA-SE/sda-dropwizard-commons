package org.sdase.commons.shared.certificates.ca;

import io.dropwizard.Configuration;
import java.util.function.Function;

/**
 * @param <C> the type of the specific {@link Configuration} used in the application
 */
@FunctionalInterface
public interface CaCertificateConfigurationProvider<C extends Configuration>
    extends Function<C, CaCertificateConfiguration> {}
