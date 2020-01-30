package org.sdase.commons.server.testing;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class EnvironmentTest {

  private static final String TEST_VAL = "TEST_VAL";
  private static final String TEST_VAR = "TEST_VAR";

  @SuppressWarnings("static-method")
  @Test
  public void testSetEnv() {
    Environment.setEnv(TEST_VAR, TEST_VAL);

    assertThat(System.getenv(TEST_VAR), equalTo(TEST_VAL));
  }

  @SuppressWarnings("static-method")
  @Test
  public void testUnsetEnv() {
    Environment.setEnv(TEST_VAR, TEST_VAL);
    Environment.unsetEnv(TEST_VAR);

    assertThat(System.getenv(TEST_VAR), equalTo(null));
  }
}
