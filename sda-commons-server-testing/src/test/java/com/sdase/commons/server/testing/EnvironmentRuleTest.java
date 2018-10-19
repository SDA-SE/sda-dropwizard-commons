package com.sdase.commons.server.testing;

import org.assertj.core.api.Assertions;
import org.junit.*;

public class EnvironmentRuleTest {

   @Rule
   public EnvironmentRule ENV = new EnvironmentRule()
         .setEnv("envForTesting", "envForTestingValue")
         .unsetEnv("envNotForTesting");


   @BeforeClass
   public static void assertVariableIsNotSetBeforeTest() {
      Assertions.assertThat(System.getenv("envForTesting")).isNull();
      Assertions.assertThat(System.getenv("envNotForTesting")).isNull();
      Environment.setEnv("envNotForTesting", "envNotForTestingValue");
   }

   @Test
   public void shouldBeSetInTest() {
      Assertions.assertThat(System.getenv("envForTesting")).isEqualTo("envForTestingValue");
   }

   @Test
   public void shouldBeUnsetInTest() {
      Assertions.assertThat(System.getenv("envNotForTesting")).isNull();
   }

   @AfterClass
   public static void assertVariableIsNotSetAfterTest() {
      Assertions.assertThat(System.getenv("envForTesting")).isNull();
      Environment.unsetEnv("envNotForTesting");
   }
}