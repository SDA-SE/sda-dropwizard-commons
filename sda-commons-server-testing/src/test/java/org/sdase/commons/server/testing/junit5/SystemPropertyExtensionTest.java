package org.sdase.commons.server.testing.junit5;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SystemPropertyExtensionTest {

  private static final String key1 = "envForTesting";
  private static final String value1 = UUID.randomUUID().toString();

  private static final String key2 = "envForTestingSupplier";
  private static final String value2 = UUID.randomUUID().toString();

  private static final String keyWithNullValue = "envNotForTesting";

  @RegisterExtension
  public static SystemPropertyExtension PROP =
      new SystemPropertyExtension()
          .setProperty(key1, value1)
          .setProperty(key2, () -> value2)
          .unsetProperty(keyWithNullValue);

  @Test
  void shouldBeSetInTest() {
    assertThat(System.getProperty(key1)).isEqualTo(value1);
  }

  @Test
  void shouldBeSetInTestWhenUsingSupplier() {
    assertThat(System.getProperty(key2)).isEqualTo(value2);
  }

  @Test
  void shouldBeUnsetInTest() {
    assertThat(System.getProperty(keyWithNullValue)).isNull();
  }
}
