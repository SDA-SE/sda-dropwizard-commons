package org.sdase.commons.server.testing.junit5;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.cli.Command;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * This class only adds {@link BeforeAllCallback} and {@link AfterAllCallback} to the original
 * {@link io.dropwizard.testing.junit5.DropwizardAppExtension}.
 */
public class DropwizardAppExtension<C extends Configuration>
    extends io.dropwizard.testing.junit5.DropwizardAppExtension<C>
    implements BeforeAllCallback, AfterAllCallback {

  public DropwizardAppExtension(Class<? extends Application<C>> applicationClass) {
    super(applicationClass);
  }

  public DropwizardAppExtension(
      Class<? extends Application<C>> applicationClass,
      @Nullable String configPath,
      ConfigOverride... configOverrides) {
    super(applicationClass, configPath, configOverrides);
  }

  public DropwizardAppExtension(
      Class<? extends Application<C>> applicationClass,
      @Nullable String configPath,
      ConfigurationSourceProvider configSourceProvider,
      ConfigOverride... configOverrides) {
    super(applicationClass, configPath, configSourceProvider, configOverrides);
  }

  public DropwizardAppExtension(
      Class<? extends Application<C>> applicationClass,
      @Nullable String configPath,
      Optional<String> customPropertyPrefix,
      ConfigOverride... configOverrides) {
    super(applicationClass, configPath, customPropertyPrefix, configOverrides);
  }

  public DropwizardAppExtension(
      Class<? extends Application<C>> applicationClass,
      @Nullable String configPath,
      @Nullable String customPropertyPrefix,
      ConfigOverride... configOverrides) {
    super(applicationClass, configPath, customPropertyPrefix, configOverrides);
  }

  public DropwizardAppExtension(
      Class<? extends Application<C>> applicationClass,
      @Nullable String configPath,
      ConfigurationSourceProvider configSourceProvider,
      @Nullable String customPropertyPrefix,
      ConfigOverride... configOverrides) {
    super(
        applicationClass, configPath, configSourceProvider, customPropertyPrefix, configOverrides);
  }

  public DropwizardAppExtension(
      Class<? extends Application<C>> applicationClass,
      @Nullable String configPath,
      Optional<String> customPropertyPrefix,
      Function<Application<C>, Command> commandInstantiator,
      ConfigOverride... configOverrides) {
    super(applicationClass, configPath, customPropertyPrefix, commandInstantiator, configOverrides);
  }

  public DropwizardAppExtension(
      Class<? extends Application<C>> applicationClass,
      @Nullable String configPath,
      @Nullable String customPropertyPrefix,
      Function<Application<C>, Command> commandInstantiator,
      ConfigOverride... configOverrides) {
    super(applicationClass, configPath, customPropertyPrefix, commandInstantiator, configOverrides);
  }

  public DropwizardAppExtension(
      Class<? extends Application<C>> applicationClass,
      @Nullable String configPath,
      ConfigurationSourceProvider configSourceProvider,
      @Nullable String customPropertyPrefix,
      Function<Application<C>, Command> commandInstantiator,
      ConfigOverride... configOverrides) {
    super(
        applicationClass,
        configPath,
        configSourceProvider,
        customPropertyPrefix,
        commandInstantiator,
        configOverrides);
  }

  public DropwizardAppExtension(Class<? extends Application<C>> applicationClass, C configuration) {
    super(applicationClass, configuration);
  }

  public DropwizardAppExtension(
      Class<? extends Application<C>> applicationClass,
      C configuration,
      Function<Application<C>, Command> commandInstantiator) {
    super(applicationClass, configuration, commandInstantiator);
  }

  public DropwizardAppExtension(DropwizardTestSupport<C> testSupport) {
    super(testSupport);
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    super.after();
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    super.before();
  }
}
