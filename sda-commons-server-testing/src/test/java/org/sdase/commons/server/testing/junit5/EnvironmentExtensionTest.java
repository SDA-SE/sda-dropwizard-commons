package org.sdase.commons.server.testing.junit5;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class EnvironmentExtensionTest {

  private static final String key1 = "envForTesting";
  private static final String value1 = UUID.randomUUID().toString();

  private static final String key2 = "envForTestingSupplier";
  private static final String value2 = UUID.randomUUID().toString();

  private static final String keyWithNullValue = "envNotForTesting";

  @RegisterExtension
  public static EnvironmentExtension PROP =
      new EnvironmentExtension()
          .setEnv(key1, value1)
          .setEnv(key2, () -> value2)
          .unsetEnv(keyWithNullValue);

  @Test
  void shouldBeSetInTest() {
    assertThat(System.getenv(key1)).isEqualTo(value1);
  }

  @Test
  void shouldBeSetInTestWhenUsingSupplier() {
    assertThat(System.getenv(key2)).isEqualTo(value2);
  }

  @Test
  void shouldBeUnsetInTest() {
    assertThat(System.getenv(keyWithNullValue)).isNull();
  }
}
