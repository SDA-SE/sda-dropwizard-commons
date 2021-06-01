package org.sdase.commons.keymgmt;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.keymgmt.manager.*;

class KeyMgmtBundleWithFailStrategyTest {

  @RegisterExtension
  @Order(0)
  static final DropwizardAppExtension<KeyMgmtBundleTestConfig> DW =
      new DropwizardAppExtension<>(
          KeyMgmtBundleTestApp.class,
          resourceFilePath("test-config.yml"),
          ConfigOverride.config("keyMgmt.apiKeysDefinitionPath", resourceFilePath("keys")),
          ConfigOverride.config("keyMgmt.mappingDefinitionPath", resourceFilePath("mappings")));

  private static KeyMgmtBundle<KeyMgmtBundleTestConfig> keyMgmtBundle;

  @BeforeAll
  static void init() {
    KeyMgmtBundleTestApp app = DW.getApplication();
    keyMgmtBundle = app.getKeyMgmtBundle();
    assertThat(keyMgmtBundle).isNotNull();
  }

  @Test
  void shouldThrowIfMappingDoesNotExists() {
    assertThrows(
        IllegalArgumentException.class, () -> keyMgmtBundle.createKeyMapper("NOTEXISTING"));
  }

  @Test
  void shouldGetKeyMappingCaseInsensitive() {
    KeyMapper upperCase = keyMgmtBundle.createKeyMapper("GENDER");
    KeyMapper lowerCase = keyMgmtBundle.createKeyMapper("gender");
    KeyMapper mixedCase = keyMgmtBundle.createKeyMapper("GeNdER");
    assertThat(upperCase)
        .isInstanceOf(MapOrFailKeyMapper.class)
        .isEqualTo(lowerCase)
        .isEqualTo(mixedCase);
  }

  @Test
  void shouldGetMappingValueToImpl() {
    KeyMapper gender = keyMgmtBundle.createKeyMapper("GENDER");
    assertThat(gender.toImpl("MALE")).isEqualTo("m");
    assertThat(gender.toImpl("male")).isEqualTo("m");
    assertThat(gender.toImpl("mAlE")).isEqualTo("m");
    assertThat(gender.toImpl("FeMaLe")).isEqualTo("F");
  }

  @Test
  void shouldGetMappingValueWithPrecedence() {
    assertThat(keyMgmtBundle.createKeyMapper("LINES_OF_BUSINESS").toImpl("HOUSEHOLD"))
        .isEqualTo("020");
  }

  @Test
  void shouldGetMappingValue() {
    assertThat(keyMgmtBundle.createKeyMapper("LINES_OF_BUSINESS").toImpl("CAR")).isEqualTo("001");
  }

  @Test
  void shouldGetMappingValueToApiCaseSensitive() {
    KeyMapper gender = keyMgmtBundle.createKeyMapper("GENDER");
    assertThat(gender.toApi("m")).isEqualTo("MALE");
    assertThat(gender.toApi("F")).isEqualTo("FEMALE");
    // fail case
    assertThrows(IllegalArgumentException.class, () -> gender.toApi("f"));
  }

  @Test
  void shouldGetMappingValueToApiPrecedence() {
    assertThat(keyMgmtBundle.createKeyMapper("SALUTATION").toApi("2")).isEqualTo("MRS");
  }

  @Test
  void shouldGetMappingValueToApi() {
    assertThat(keyMgmtBundle.createKeyMapper("SALUTATION").toApi("0")).isEqualTo("MR");
  }

  @Test
  void shouldMapBiDirectionalMappings() {
    KeyMapper keyMapper = keyMgmtBundle.createKeyMapper("BIDIRECTIONAL_ONLY");
    // fail case
    assertThrows(IllegalArgumentException.class, () -> keyMapper.toApi("A"));
    // fail case
    assertThrows(IllegalArgumentException.class, () -> keyMapper.toImpl("B"));
    assertThat(keyMapper.toImpl("A")).isEqualTo("B");
    assertThat(keyMapper.toApi("B")).isEqualTo("A");
  }

  public static class KeyMgmtBundleTestApp extends Application<KeyMgmtBundleTestConfig> {

    private final KeyMgmtBundle<KeyMgmtBundleTestConfig> keyMgmt =
        KeyMgmtBundle.builder()
            .withKeyMgmtConfigProvider(KeyMgmtBundleTestConfig::getKeyMgmt)
            .withFailStrategy(KeyMgmtBundle.FailStrategy.FAIL_WITH_EXCEPTION)
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
