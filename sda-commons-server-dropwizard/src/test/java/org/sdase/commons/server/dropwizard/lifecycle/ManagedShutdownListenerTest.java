package org.sdase.commons.server.dropwizard.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class ManagedShutdownListenerTest {

  @Test
  void delegateToOnShutdownOnStop() throws Exception {
    AtomicBoolean shutdownCalled = new AtomicBoolean();
    Managed managedObject = ManagedShutdownListener.onShutdown(() -> shutdownCalled.set(true));
    managedObject.stop();
    assertTrue(shutdownCalled.get());
  }

  @Test
  void doNotDelegateToOnShutdownOnStart() throws Exception {
    AtomicBoolean shutdownCalled = new AtomicBoolean();
    Managed managedObject = ManagedShutdownListener.onShutdown(() -> shutdownCalled.set(true));
    managedObject.start();
    assertFalse(shutdownCalled.get());
  }
}
