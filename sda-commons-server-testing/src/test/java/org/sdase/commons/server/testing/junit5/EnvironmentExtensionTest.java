package org.sdase.commons.server.testing.junit5;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.testing.Environment;

class EnvironmentExtensionTest {

  @RegisterExtension
  public EnvironmentExtension env =
      new EnvironmentExtension()
          .setEnv("envForTesting", "envForTestingValue")
          .setEnv("envForTestingSupplier", () -> "envForTestingSupplierValue")
          .unsetEnv("envNotForTesting");

  @BeforeAll
  public static void assertVariableIsNotSetBeforeTest() {
    assertThat(System.getenv())
        .doesNotContainKeys("envForTesting", "envNotForTesting", "envForTestingSupplier");
    Environment.setEnv("envNotForTesting", "envNotForTestingValue");
  }

  @Test
  void shouldBeSetInTest() {
    assertThat(System.getenv("envForTesting")).isEqualTo("envForTestingValue");
  }

  @Test
  void shouldBeSetInTestWhenUsingSupplier() {
    assertThat(System.getenv("envForTestingSupplier")).isEqualTo("envForTestingSupplierValue");
  }

  @Test
  void shouldBeUnsetInTest() {
    assertThat(System.getenv("envNotForTesting")).isNull();
  }

  @AfterAll
  public static void assertVariableIsNotSetAfterTest() {
    assertThat(System.getenv("envForTesting")).isNull();
    assertThat(System.getenv("envNotForTesting")).isEqualTo("envNotForTestingValue");
    Environment.unsetEnv("envNotForTesting");
  }
}
