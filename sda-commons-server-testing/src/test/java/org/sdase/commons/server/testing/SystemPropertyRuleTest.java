package org.sdase.commons.server.testing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class SystemPropertyRuleTest {

  @Rule
  public SystemPropertyRule PROP =
      new SystemPropertyRule()
          .setProperty("envForTesting", "envForTestingValue")
          .setProperty("envForTestingSupplier", () -> "envForTestingSupplierValue")
          .unsetProperty("envNotForTesting");

  @BeforeClass
  public static void assertVariableIsNotSetBeforeTest() {
    assertThat(System.getProperty("envForTesting")).isNull();
    assertThat(System.getProperty("envNotForTesting")).isNull();
    assertThat(System.getProperty("envForTestingSupplier")).isNull();
    System.setProperty("envNotForTesting", "envNotForTestingValue");
  }

  @Test
  public void shouldBeSetInTest() {
    assertThat(System.getProperty("envForTesting")).isEqualTo("envForTestingValue");
  }

  @Test
  public void shouldBeSetInTestWhenUsingSupplier() {
    assertThat(System.getProperty("envForTestingSupplier")).isEqualTo("envForTestingSupplierValue");
  }

  @Test
  public void shouldBeUnsetInTest() {
    assertThat(System.getProperty("envNotForTesting")).isNull();
  }

  @AfterClass
  public static void assertVariableIsNotSetAfterTest() {
    assertThat(System.getProperty("envForTesting")).isNull();
    assertThat(System.getProperty("envForTestingSupplier")).isNull();
    assertThat(System.getProperty("envNotForTesting")).isEqualTo("envNotForTestingValue");
    System.clearProperty("envNotForTesting");
  }
}
