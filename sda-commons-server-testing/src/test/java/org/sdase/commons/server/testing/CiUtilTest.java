package org.sdase.commons.server.testing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

class CiUtilTest {

  private final CiUtil ciUtil = new CiUtil(Set.of("RUN_IN_CI"));

  @Test
  @SetSystemProperty(key = "RUN_IN_CI", value = "yeah")
  void shouldAssumeItRunsInCiWhenEnvMatches() {
    assertThat(ciUtil.isRunningInCiPipeline()).isTrue();
  }

  @Test
  void shouldNotAssumeItRunsInCiWhenEnvIsNotPresent() {
    assertThat(ciUtil.isRunningInCiPipeline()).isFalse();
  }
}
