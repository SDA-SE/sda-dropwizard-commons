package org.sdase.commons.server.testing;

import java.util.Set;
import org.sdase.commons.server.dropwizard.bundles.SystemPropertyAndEnvironmentLookup;

public class CiUtil {

  private final SystemPropertyAndEnvironmentLookup lookup =
      new SystemPropertyAndEnvironmentLookup();

  private final Set<String> envs;

  public CiUtil() {
    envs =
        Set.of(
            // Jenkins:
            // https://www.jenkins.io/doc/book/pipeline/jenkinsfile/#using-environment-variables
            "JENKINS_HOME",
            // Github Actions:
            // https://docs.github.com/en/actions/learn-github-actions/variables#default-environment-variables
            "CI");
  }

  CiUtil(Set<String> envs) {
    this.envs = envs;
  }

  /**
   * @return {@code true} if we can detect that the current build is executed in a CI pipeline
   */
  public boolean isRunningInCiPipeline() {
    return envs.stream().anyMatch(env -> lookup.lookup(env) != null);
  }
}
