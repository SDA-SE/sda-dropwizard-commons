package org.sdase.commons.keymgmt;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.io.IOException;
import java.util.*;
import org.sdase.commons.keymgmt.config.KeyMgmtConfig;
import org.sdase.commons.keymgmt.config.KeyMgmtConfigProvider;
import org.sdase.commons.keymgmt.manager.*;
import org.sdase.commons.keymgmt.model.KeyDefinition;
import org.sdase.commons.keymgmt.model.KeyMappingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyMgmtBundle<T extends Configuration> implements ConfiguredBundle<T> {

  private static final Logger LOG = LoggerFactory.getLogger(KeyMgmtBundle.class);

  private final KeyMgmtConfigProvider<T> configProvider;

  private Map<String, KeyDefinition> keys;
  private Map<String, KeyMappingModel> mappings;
  private boolean initialized = false;

  public static InitialBuilder builder() {
    return new Builder<>();
  }

  private KeyMgmtBundle(KeyMgmtConfigProvider<T> configProvider) {
    this.configProvider = configProvider;
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // no init
  }

  @Override
  public void run(T configuration, Environment environment) throws IOException {
    KeyMgmtConfig config = configProvider.apply(configuration);

    keys = ModelReader.parseApiKeys(config.getApiKeysDefinitionPath());
    mappings = ModelReader.parseMappingFile(config.getMappingDefinitionPath());
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
    if (mappings.containsKey(keyDefinitionName)) {
      return new MapOrPassthroughKeyMapper(mappings.get(keyDefinitionName));
    } else {
      return new PassthroughKeyMapper();
    }
  }

  /** @return a set with all known key definition names */
  public Set<String> getKeyDefinitionNames() {
    return keys.keySet();
  }

  /** @return a set with all known key definition names for that mappings exists */
  public Set<String> getMappingKeyDefinitionNames() {
    return mappings.keySet();
  }

  // --------------
  // ----- Builder
  // --------------
  public interface InitialBuilder {
    <C extends Configuration> FinalBuilder<C> withKeyMgmtConfigProvider(
        KeyMgmtConfigProvider<C> configProvider);
  }

  public interface FinalBuilder<C extends Configuration> {
    KeyMgmtBundle<C> build();
  }

  public static class Builder<C extends Configuration> implements InitialBuilder, FinalBuilder<C> {

    private KeyMgmtConfigProvider<C> keyMgmtConfigProvider;

    private Builder() {
      // private method to prevent external instantiation
    }

    private Builder(KeyMgmtConfigProvider<C> keyMgmtConfigProvider) {
      this.keyMgmtConfigProvider = keyMgmtConfigProvider;
    }

    @Override
    public KeyMgmtBundle<C> build() {
      return new KeyMgmtBundle<>(keyMgmtConfigProvider);
    }

    @Override
    public <E extends Configuration> FinalBuilder<E> withKeyMgmtConfigProvider(
        KeyMgmtConfigProvider<E> configProvider) {
      return new Builder<>(configProvider);
    }
  }
}
