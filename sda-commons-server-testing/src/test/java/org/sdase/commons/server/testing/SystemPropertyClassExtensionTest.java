package org.sdase.commons.server.testing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;

class SystemPropertyClassExtensionTest {

  @RegisterExtension
  @Order(0)
  static final PreAndPostConditions PRE_AND_POST_CONDITIONS = new PreAndPostConditions();

  @RegisterExtension
  @Order(1)
  static final SystemPropertyClassExtension PROP =
      new SystemPropertyClassExtension()
          .setProperty("envForTesting", "envForTestingValue")
          .setProperty("envForTestingSupplier", () -> "envForTestingSupplierValue")
          .unsetProperty("envNotForTesting");

  @Test
  void shouldBeSetInTest() {
    assertThat(System.getProperty("envForTesting")).isEqualTo("envForTestingValue");
  }

  @Test
  void shouldBeSetInTestWhenUsingSupplier() {
    assertThat(System.getProperty("envForTestingSupplier")).isEqualTo("envForTestingSupplierValue");
  }

  @Test
  void shouldBeUnsetInTest() {
    assertThat(System.getProperty("envNotForTesting")).isNull();
  }

  private static class PreAndPostConditions implements BeforeAllCallback, AfterAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
      assertThat(System.getProperty("envForTesting")).isNull();
      assertThat(System.getProperty("envNotForTesting")).isNull();
      assertThat(System.getProperty("envForTestingSupplier")).isNull();
      System.setProperty("envNotForTesting", "envNotForTestingValue");
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
      assertThat(System.getProperty("envForTesting")).isNull();
      assertThat(System.getProperty("envForTestingSupplier")).isNull();
      assertThat(System.getProperty("envNotForTesting")).isEqualTo("envNotForTestingValue");
      System.clearProperty("envNotForTesting");
    }
  }
}
