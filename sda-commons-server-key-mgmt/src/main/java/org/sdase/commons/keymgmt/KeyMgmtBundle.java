package org.sdase.commons.keymgmt;

import io.dropwizard.core.Configuration;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import java.io.IOException;
import java.util.*;
import org.sdase.commons.keymgmt.config.KeyMgmtConfig;
import org.sdase.commons.keymgmt.config.KeyMgmtConfigProvider;
import org.sdase.commons.keymgmt.manager.*;
import org.sdase.commons.keymgmt.model.KeyDefinition;
import org.sdase.commons.keymgmt.model.KeyMappingModel;
import org.sdase.commons.keymgmt.validator.KeyMgmtBundleHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyMgmtBundle<T extends Configuration> implements ConfiguredBundle<T> {

  private static final Logger LOG = LoggerFactory.getLogger(KeyMgmtBundle.class);

  private final KeyMgmtConfigProvider<T> configProvider;
  private final FailStrategy failStrategy;

  private Map<String, KeyDefinition> keys;
  private Map<String, KeyMappingModel> mappings;
  private boolean initialized = false;
  private KeyMgmtConfig config;

  public static InitialBuilder builder() {
    return new Builder<>();
  }

  private KeyMgmtBundle(KeyMgmtConfigProvider<T> configProvider, FailStrategy failStrategy) {
    this.configProvider = configProvider;
    this.failStrategy = failStrategy;
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // no init
  }

  @Override
  public void run(T configuration, Environment environment) throws IOException {
    config = configProvider.apply(configuration);

    keys = ModelReader.parseApiKeys(config.getApiKeysDefinitionPath());
    mappings = ModelReader.parseMappingFile(config.getMappingDefinitionPath());
    KeyMgmtBundleHolder.setKeyMgmtBundle(this);
    initialized = true;
  }

  public KeyManager createKeyManager(String keyDefinitionName) {
    if (!initialized) {
      throw new IllegalStateException(
          "KeyManager can be build in run(C, Environment), not in initialize(Bootstrap)");
    }
    if (keys.containsKey(keyDefinitionName)) {
      return new KeyManagerImpl(keys.get(keyDefinitionName));
    }
    LOG.warn("Requested key manager for non existing key definition");
    return new NoKeyKeyManager();
  }

  public KeyMapper createKeyMapper(String keyDefinitionName) {
    if (!initialized) {
      throw new IllegalStateException(
          "KeyMapper can be build in run(C, Environment), not in initialize(Bootstrap)");
    }
    return switch (failStrategy) {
      case FAIL_WITH_EXCEPTION -> getMapOrFailKeyMapper(keyDefinitionName, false, false);
      case PLACEHOLDER_BIDIRECTIONAL -> getMapOrFailKeyMapper(keyDefinitionName, true, true);
      case PLACEHOLDER_API_TO_IMPL -> getMapOrFailKeyMapper(keyDefinitionName, true, false);
      case PLACEHOLDER_IMPL_TO_API -> getMapOrFailKeyMapper(keyDefinitionName, false, true);
      default -> getMapOrPassthroughKeyMapper(keyDefinitionName);
    };
  }

  private MapOrFailKeyMapper getMapOrFailKeyMapper(
      String keyDefinitionName, boolean useApiPlaceholder, boolean useImplPlaceholder) {
    if (keyDefinitionName != null && mappings.containsKey(keyDefinitionName)) {
      return new MapOrFailKeyMapper(
          mappings.get(keyDefinitionName), useApiPlaceholder, useImplPlaceholder);
    } else {
      throw new IllegalArgumentException(
          String.format("No mapping found for key '%s'", keyDefinitionName));
    }
  }

  private KeyMapper getMapOrPassthroughKeyMapper(String keyDefinitionName) {
    if (mappings.containsKey(keyDefinitionName)) {
      return new MapOrPassthroughKeyMapper(mappings.get(keyDefinitionName));
    } else {
      return new PassthroughKeyMapper();
    }
  }

  /**
   * determines if the bundle should validate incoming key values
   *
   * @return true if the bundle validates
   */
  public boolean isValueValidationEnabled() {
    return !config.isDisableValidation();
  }

  /**
   * @return a set with all known key definition names
   */
  public Set<String> getKeyDefinitionNames() {
    return keys.keySet();
  }

  /**
   * @return a set with all known key definition names for that mappings exists
   */
  public Set<String> getMappingKeyDefinitionNames() {
    return mappings.keySet();
  }

  // --------------
  // ----- Builder
  // --------------
  public enum FailStrategy {
    PASSTHROUGH,
    FAIL_WITH_EXCEPTION,
    PLACEHOLDER_BIDIRECTIONAL,
    PLACEHOLDER_IMPL_TO_API,
    PLACEHOLDER_API_TO_IMPL
  }

  public interface InitialBuilder {
    <C extends Configuration> FinalBuilder<C> withKeyMgmtConfigProvider(
        KeyMgmtConfigProvider<C> configProvider);
  }

  public interface FinalBuilder<C extends Configuration> {

    FinalBuilder<C> withFailStrategy(FailStrategy strategy);

    KeyMgmtBundle<C> build();
  }

  public static class Builder<C extends Configuration> implements InitialBuilder, FinalBuilder<C> {

    private KeyMgmtConfigProvider<C> keyMgmtConfigProvider;
    private FailStrategy failStrategy = FailStrategy.PASSTHROUGH;

    private Builder() {
      // private method to prevent external instantiation
    }

    private Builder(KeyMgmtConfigProvider<C> keyMgmtConfigProvider) {
      this.keyMgmtConfigProvider = keyMgmtConfigProvider;
    }

    @Override
    public FinalBuilder<C> withFailStrategy(FailStrategy strategy) {
      this.failStrategy = strategy;
      return this;
    }

    @Override
    public KeyMgmtBundle<C> build() {
      return new KeyMgmtBundle<>(keyMgmtConfigProvider, failStrategy);
    }

    @Override
    public <E extends Configuration> FinalBuilder<E> withKeyMgmtConfigProvider(
        KeyMgmtConfigProvider<E> configProvider) {
      return new Builder<>(configProvider);
    }
  }
}
