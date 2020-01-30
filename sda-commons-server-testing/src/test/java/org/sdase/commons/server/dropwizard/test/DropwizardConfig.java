package org.sdase.commons.server.dropwizard.test;

import io.dropwizard.Configuration;

public class DropwizardConfig extends Configuration {

  private String envReplaced;

  private String envDefault;

  private String envWithDefaultReplaced;

  private String envMissing;

  private OptionalConfig optionalConfig;

  private String example;

  public String getEnvReplaced() {
    return envReplaced;
  }

  public DropwizardConfig setEnvReplaced(String envReplaced) {
    this.envReplaced = envReplaced;
    return this;
  }

  public String getEnvDefault() {
    return envDefault;
  }

  public DropwizardConfig setEnvDefault(String envDefault) {
    this.envDefault = envDefault;
    return this;
  }

  public String getEnvWithDefaultReplaced() {
    return envWithDefaultReplaced;
  }

  public DropwizardConfig setEnvWithDefaultReplaced(String envWithDefaultReplaced) {
    this.envWithDefaultReplaced = envWithDefaultReplaced;
    return this;
  }

  public String getEnvMissing() {
    return envMissing;
  }

  public DropwizardConfig setEnvMissing(String envMissing) {
    this.envMissing = envMissing;
    return this;
  }

  public String getExample() {
    return example;
  }

  public DropwizardConfig setExample(String example) {
    this.example = example;
    return this;
  }

  public OptionalConfig getOptionalConfig() {
    return optionalConfig;
  }

  public DropwizardConfig setOptionalConfig(OptionalConfig optionalConfig) {
    this.optionalConfig = optionalConfig;
    return this;
  }

  public static class OptionalConfig {

    private String property1;
    private String property2;

    public String getProperty1() {
      return property1;
    }

    public OptionalConfig setProperty1(String property1) {
      this.property1 = property1;
      return this;
    }

    public String getProperty2() {
      return property2;
    }

    public OptionalConfig setProperty2(String property2) {
      this.property2 = property2;
      return this;
    }
  }
}
