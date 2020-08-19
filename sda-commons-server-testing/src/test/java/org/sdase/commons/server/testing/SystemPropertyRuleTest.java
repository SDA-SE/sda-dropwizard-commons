package org.sdase.commons.server.testing;

import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class SystemPropertyRuleTest {

  @Rule
  public SystemPropertyRule PROP =
      new SystemPropertyRule()
          .setProperty("envForTesting", "envForTestingValue")
          .unsetProperty("envNotForTesting");

  @BeforeClass
  public static void assertVariableIsNotSetBeforeTest() {
    Assertions.assertThat(System.getProperty("envForTesting")).isNull();
    Assertions.assertThat(System.getProperty("envNotForTesting")).isNull();
    System.setProperty("envNotForTesting", "envNotForTestingValue");
  }

  @Test
  public void shouldBeSetInTest() {
    Assertions.assertThat(System.getProperty("envForTesting")).isEqualTo("envForTestingValue");
  }

  @Test
  public void shouldBeUnsetInTest() {
    Assertions.assertThat(System.getProperty("envNotForTesting")).isNull();
  }

  @AfterClass
  public static void assertVariableIsNotSetAfterTest() {
    Assertions.assertThat(System.getProperty("envForTesting")).isNull();
    Assertions.assertThat(System.getProperty("envNotForTesting"))
        .isEqualTo("envNotForTestingValue");
    System.clearProperty("envNotForTesting");
  }
}
