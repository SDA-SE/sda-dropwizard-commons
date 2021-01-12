package org.sdase.commons.server.testing.builder;

import io.dropwizard.Configuration;
import io.dropwizard.testing.junit5.DropwizardAppExtension;

public interface DropwizardExtensionBuilders extends DropwizardBuilders {

  interface CustomizationBuilder<C extends Configuration>
      extends DropwizardBuilders.CustomizationBuilder<C> {

    /**
     * @return the {@link DropwizardAppExtension} using {@link Configuration} of type {@code C}
     *     configured with this builder
     */
    DropwizardAppExtension<C> build();
  }
}
