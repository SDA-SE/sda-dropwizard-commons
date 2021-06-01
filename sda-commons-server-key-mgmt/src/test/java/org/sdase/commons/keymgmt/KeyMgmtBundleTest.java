package org.sdase.commons.keymgmt;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.keymgmt.manager.KeyManager;
import org.sdase.commons.keymgmt.manager.KeyMapper;
import org.sdase.commons.keymgmt.manager.NoKeyKeyManager;
import org.sdase.commons.keymgmt.manager.PassthroughKeyMapper;

class KeyMgmtBundleTest {

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
  void shouldGetNoKeyKeyManagerIfKeyDoesNotExist() {
    assertThat(keyMgmtBundle.createKeyManager("NOTEXISTING")).isInstanceOf(NoKeyKeyManager.class);
  }

  @Test
  void shouldGetPassthroughKeyMapperManagerIfMappingDoesNotExists() {
    assertThat(keyMgmtBundle.createKeyMapper("NOTEXISTING"))
        .isInstanceOf(PassthroughKeyMapper.class);
  }

  @Test
  void shouldGetKeyManagerManagerCaseInsensitive() {
    KeyManager upperCase = keyMgmtBundle.createKeyManager("GENDER");
    KeyManager lowerCase = keyMgmtBundle.createKeyManager("gender");
    KeyManager mixedCase = keyMgmtBundle.createKeyManager("GeNdER");
    assertThat(upperCase).isEqualTo(lowerCase).isEqualTo(mixedCase);
  }

  @Test
  void shouldGetKeyMappingCaseInsensitive() {
    KeyMapper upperCase = keyMgmtBundle.createKeyMapper("GENDER");
    KeyMapper lowerCase = keyMgmtBundle.createKeyMapper("gender");
    KeyMapper mixedCase = keyMgmtBundle.createKeyMapper("GeNdER");
    assertThat(upperCase).isEqualTo(lowerCase).isEqualTo(mixedCase);
  }

  @Test
  void shouldCheckKeyValuesCaseInsensitive() {
    KeyManager gender = keyMgmtBundle.createKeyManager("GENDER");
    assertThat(gender.isValidValue("MALE")).isTrue();
    assertThat(gender.isValidValue("Male")).isTrue();
    assertThat(gender.isValidValue("male")).isTrue();
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
    // pass through case
    assertThat(gender.toApi("f")).isEqualTo("f");
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
  void shouldConfirmValidKey() {
    assertThat(keyMgmtBundle.createKeyManager("GENDER").isValidValue("MALE")).isTrue();
    assertThat(keyMgmtBundle.createKeyManager("GENDER").isValidValue("MaLe")).isTrue();
    assertThat(keyMgmtBundle.createKeyManager("GENDER").isValidValue("male")).isTrue();
  }

  @Test
  void shouldDeclineInvalidKey() {
    assertThat(keyMgmtBundle.createKeyManager("GENDER").isValidValue("Mr")).isFalse();
  }

  @Test
  void shouldReturnListOfValidValues() {
    assertThat(keyMgmtBundle.createKeyManager("GENDER").getValidValues())
        .containsExactlyInAnyOrder("MALE", "FEMALE", "OTHER");
  }

  @Test
  void shouldReturnFalseIfKeyNotEmpty() {
    assertThat(keyMgmtBundle.createKeyManager("NONEXISTING").getValidValues()).isEmpty();
  }

  @Test
  void shouldMapBiDirectionalMappings() {
    assertThat(keyMgmtBundle.createKeyMapper("BIDIRECTIONAL_ONLY").toApi("A"))
        .isEqualTo("A"); // Pass through
    assertThat(keyMgmtBundle.createKeyMapper("BIDIRECTIONAL_ONLY").toImpl("B"))
        .isEqualTo("B"); // Pass through
    assertThat(keyMgmtBundle.createKeyMapper("BIDIRECTIONAL_ONLY").toImpl("A")).isEqualTo("B");
    assertThat(keyMgmtBundle.createKeyMapper("BIDIRECTIONAL_ONLY").toApi("B")).isEqualTo("A");
  }

  @Test
  void shouldGetAllKeys() {
    assertThat(keyMgmtBundle.getKeyDefinitionNames())
        .containsExactlyInAnyOrder("GENDER", "SALUTATION");
  }

  @Test
  void shouldGetAllMappings() {
    assertThat(keyMgmtBundle.getMappingKeyDefinitionNames())
        .containsExactlyInAnyOrder(
            "GENDER", "SALUTATION", "LINES_OF_BUSINESS", "BIDIRECTIONAL_ONLY");
  }
}
