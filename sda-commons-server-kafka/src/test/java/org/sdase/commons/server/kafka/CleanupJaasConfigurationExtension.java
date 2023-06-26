package org.sdase.commons.server.kafka;

import javax.security.auth.login.Configuration;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class CleanupJaasConfigurationExtension implements BeforeAllCallback, AfterAllCallback {

  @Override
  public void afterAll(ExtensionContext context) {
    Configuration.setConfiguration(null);
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    Configuration.setConfiguration(null);
  }
}
