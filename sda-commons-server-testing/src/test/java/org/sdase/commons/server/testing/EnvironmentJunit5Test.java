package org.sdase.commons.server.testing;


import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EnvironmentJunit5Test {

  private static final String TEST_VAL = "TEST_VAL";
  private static final String TEST_VAR = "TEST_VAR";

  @Test
  void testSetEnv() {
    Environment.setEnv(TEST_VAR, TEST_VAL);

    assertThat(System.getenv(TEST_VAR)).isEqualTo(TEST_VAL);
  }

  @Test
  void testUnsetEnv() {
    Environment.setEnv(TEST_VAR, TEST_VAL);
    Environment.unsetEnv(TEST_VAR);

    assertThat(System.getenv(TEST_VAR)).isNull();
  }
}
