package org.sdase.commons.server.testing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.*;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

@DisabledForJreRange(min = JRE.JAVA_16)
public class EnvironmentRuleTest {

  @Rule
  public EnvironmentRule ENV =
      new EnvironmentRule()
          .setEnv("envForTesting", "envForTestingValue")
          .setEnv("envForTestingSupplier", () -> "envForTestingSupplierValue")
          .unsetEnv("envNotForTesting");

  @BeforeClass
  public static void assertVariableIsNotSetBeforeTest() {
    assertThat(System.getenv())
        .doesNotContainKeys("envForTesting", "envNotForTesting", "envForTestingSupplier");
    Environment.setEnv("envNotForTesting", "envNotForTestingValue");
  }

  @Test
  public void shouldBeSetInTest() {
    assertThat(System.getenv("envForTesting")).isEqualTo("envForTestingValue");
  }

  @Test
  public void shouldBeSetInTestWhenUsingSupplier() {
    assertThat(System.getenv("envForTestingSupplier")).isEqualTo("envForTestingSupplierValue");
  }

  @Test
  public void shouldBeUnsetInTest() {
    assertThat(System.getenv("envNotForTesting")).isNull();
  }

  @AfterClass
  public static void assertVariableIsNotSetAfterTest() {
    assertThat(System.getenv("envForTesting")).isNull();
    assertThat(System.getenv("envNotForTesting")).isEqualTo("envNotForTestingValue");
    Environment.unsetEnv("envNotForTesting");
  }
}
