package org.sdase.commons.server.testing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class EnvironmentTest {

  private static final String TEST_VAL = "TEST_VAL";
  private static final String TEST_VAR = "TEST_VAR";

  @SuppressWarnings("static-method")
  @Test
  public void testSetEnv() {
    Environment.setEnv(TEST_VAR, TEST_VAL);

    assertThat(System.getenv(TEST_VAR)).isEqualTo(TEST_VAL);
  }

  @SuppressWarnings("static-method")
  @Test
  public void testUnsetEnv() {
    Environment.setEnv(TEST_VAR, TEST_VAL);
    Environment.unsetEnv(TEST_VAR);

    assertThat(System.getenv(TEST_VAR)).isNull();
  }
}
