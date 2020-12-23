package org.sdase.commons.server.testing.junit5;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import javax.ws.rs.client.Client;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DropwizardJunit5Helper {

  private DropwizardJunit5Helper() {}

  public static <C extends Configuration> DropwizardAppExtensionWithCallbacks<C> addCallbacks(
      DropwizardAppExtension<C> extension) {
    return new DropwizardAppExtensionWithCallbacks<>(extension);
  }

  public static class DropwizardAppExtensionWithCallbacks<C extends Configuration>
      implements BeforeAllCallback, AfterAllCallback {

    private final DropwizardAppExtension<C> delegate;

    public DropwizardAppExtensionWithCallbacks(DropwizardAppExtension<C> extension) {
      this.delegate = extension;
    }

    @Override
    public void afterAll(ExtensionContext context) {
      delegate.after();
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
      delegate.before();
    }

    public Application<C> getApplication() {
      return delegate.getApplication();
    }

    public Environment getEnvironment() {
      return delegate.getEnvironment();
    }

    public C getConfiguration() {
      return delegate.getConfiguration();
    }

    public int getLocalPort() {
      return delegate.getLocalPort();
    }

    public int getAdminPort() {
      return delegate.getAdminPort();
    }

    public ObjectMapper getObjectMapper() {
      return delegate.getObjectMapper();
    }

    public DropwizardTestSupport<C> getTestSupport() {
      return delegate.getTestSupport();
    }

    public Client client() {
      return delegate.client();
    }
  }
}
