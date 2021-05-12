package org.sdase.commons.keymgmt;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class KeyMgmtBundleTestApp extends Application<KeyMgmtBundleTestConfig> {

  private final KeyMgmtBundle<KeyMgmtBundleTestConfig> keyMgmt =
      KeyMgmtBundle.builder()
          .withKeyMgmtConfigProvider(KeyMgmtBundleTestConfig::getKeyMgmt)
          .build();

  @Override
  public void initialize(Bootstrap<KeyMgmtBundleTestConfig> bootstrap) {
    bootstrap.addBundle(keyMgmt);
  }

  @Override
  public void run(KeyMgmtBundleTestConfig configuration, Environment environment) {
    // nothing here
  }

  public KeyMgmtBundle<KeyMgmtBundleTestConfig> getKeyMgmtBundle() {
    return keyMgmt;
  }
}
