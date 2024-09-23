package org.sdase.commons.keymgmt;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.keymgmt.manager.KeyMapper;

class KeyMgmtBundleWithPlaceholderTest {

  @RegisterExtension
  @Order(0)
  static final DropwizardAppExtension<KeyMgmtBundleTestConfig> bidirectionalDW =
      new DropwizardAppExtension<>(
          BidirectionalTestApp.class,
          resourceFilePath("test-config.yml"),
          ConfigOverride.config("keyMgmt.apiKeysDefinitionPath", resourceFilePath("keys")),
          ConfigOverride.config("keyMgmt.mappingDefinitionPath", resourceFilePath("mappings")));

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<KeyMgmtBundleTestConfig> apiToImplDW =
      new DropwizardAppExtension<>(
          ApiToImplTestApp.class,
          resourceFilePath("test-config.yml"),
          ConfigOverride.config("keyMgmt.apiKeysDefinitionPath", resourceFilePath("keys")),
          ConfigOverride.config("keyMgmt.mappingDefinitionPath", resourceFilePath("mappings")));

  @RegisterExtension
  @Order(2)
  static final DropwizardAppExtension<KeyMgmtBundleTestConfig> implToApiDW =
      new DropwizardAppExtension<>(
          ImplToApiTestApp.class,
          resourceFilePath("test-config.yml"),
          ConfigOverride.config("keyMgmt.apiKeysDefinitionPath", resourceFilePath("keys")),
          ConfigOverride.config("keyMgmt.mappingDefinitionPath", resourceFilePath("mappings")));

  private static KeyMgmtBundle<KeyMgmtBundleTestConfig> bidirectionalKeyMgmtBundle;

  private static KeyMgmtBundle<KeyMgmtBundleTestConfig> apiToImplKeyMgmtBundle;

  private static KeyMgmtBundle<KeyMgmtBundleTestConfig> implToApiKeyMgmtBundle;

  @BeforeAll
  static void init() {
    bidirectionalKeyMgmtBundle =
        ((BidirectionalTestApp) bidirectionalDW.getApplication()).getKeyMgmtBundle();
    assertThat(bidirectionalKeyMgmtBundle).isNotNull();
    apiToImplKeyMgmtBundle = ((ApiToImplTestApp) apiToImplDW.getApplication()).getKeyMgmtBundle();
    assertThat(apiToImplKeyMgmtBundle).isNotNull();
    implToApiKeyMgmtBundle = ((ImplToApiTestApp) implToApiDW.getApplication()).getKeyMgmtBundle();
    assertThat(implToApiKeyMgmtBundle).isNotNull();
  }

  @Test
  void shouldMapBiDirectionalMappings() {
    KeyMapper keyMapper = bidirectionalKeyMgmtBundle.createKeyMapper("BIDIRECTIONAL_ONLY");
    // success case
    assertThat(keyMapper.toImpl("A")).isEqualTo("B");
    assertThat(keyMapper.toApi("B")).isEqualTo("A");
    // fail case
    assertThat(keyMapper.toImpl("B")).isEqualTo("UNKNOWN_KEY_VALUE");
    assertThat(keyMapper.toApi("A")).isEqualTo("UNKNOWN_KEY_VALUE");
  }

  @Test
  void shouldMapApiToImplMappings() {
    KeyMapper keyMapper = apiToImplKeyMgmtBundle.createKeyMapper("BIDIRECTIONAL_ONLY");
    // success case
    assertThat(keyMapper.toImpl("A")).isEqualTo("B");
    // fail case
    assertThat(keyMapper.toImpl("B")).isEqualTo("UNKNOWN_KEY_VALUE");
  }

  @Test
  void shouldMapImplToApiMappings() {
    KeyMapper keyMapper = implToApiKeyMgmtBundle.createKeyMapper("BIDIRECTIONAL_ONLY");
    // success case
    assertThat(keyMapper.toApi("B")).isEqualTo("A");
    // fail case
    assertThat(keyMapper.toApi("A")).isEqualTo("UNKNOWN_KEY_VALUE");
  }

  public static class BidirectionalTestApp extends Application<KeyMgmtBundleTestConfig> {

    private final KeyMgmtBundle<KeyMgmtBundleTestConfig> keyMgmt =
        KeyMgmtBundle.builder()
            .withKeyMgmtConfigProvider(KeyMgmtBundleTestConfig::getKeyMgmt)
            .withFailStrategy(KeyMgmtBundle.FailStrategy.PLACEHOLDER_BIDIRECTIONAL)
            .build();

    @Override
    public void initialize(Bootstrap<KeyMgmtBundleTestConfig> bootstrap) {
      bootstrap.addBundle(keyMgmt);
    }

    @Override
    public void run(KeyMgmtBundleTestConfig configuration, Environment environment) {
      // nothing here
    }

    public KeyMgmtBundle<KeyMgmtBundleTestConfig> getKeyMgmtBundle() {
      return keyMgmt;
    }
  }

  public static class ApiToImplTestApp extends Application<KeyMgmtBundleTestConfig> {

    private final KeyMgmtBundle<KeyMgmtBundleTestConfig> keyMgmt =
        KeyMgmtBundle.builder()
            .withKeyMgmtConfigProvider(KeyMgmtBundleTestConfig::getKeyMgmt)
            .withFailStrategy(KeyMgmtBundle.FailStrategy.PLACEHOLDER_API_TO_IMPL)
            .build();

    @Override
    public void initialize(Bootstrap<KeyMgmtBundleTestConfig> bootstrap) {
      bootstrap.addBundle(keyMgmt);
    }

    @Override
    public void run(KeyMgmtBundleTestConfig configuration, Environment environment) {
      // nothing here
    }

    public KeyMgmtBundle<KeyMgmtBundleTestConfig> getKeyMgmtBundle() {
      return keyMgmt;
    }
  }

  public static class ImplToApiTestApp extends Application<KeyMgmtBundleTestConfig> {

    private final KeyMgmtBundle<KeyMgmtBundleTestConfig> keyMgmt =
        KeyMgmtBundle.builder()
            .withKeyMgmtConfigProvider(KeyMgmtBundleTestConfig::getKeyMgmt)
            .withFailStrategy(KeyMgmtBundle.FailStrategy.PLACEHOLDER_IMPL_TO_API)
            .build();

    @Override
    public void initialize(Bootstrap<KeyMgmtBundleTestConfig> bootstrap) {
      bootstrap.addBundle(keyMgmt);
    }

    @Override
    public void run(KeyMgmtBundleTestConfig configuration, Environment environment) {
      // nothing here
    }

    public KeyMgmtBundle<KeyMgmtBundleTestConfig> getKeyMgmtBundle() {
      return keyMgmt;
    }
  }
}
