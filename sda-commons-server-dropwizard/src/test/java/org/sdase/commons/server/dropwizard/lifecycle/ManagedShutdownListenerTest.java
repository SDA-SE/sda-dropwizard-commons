package org.sdase.commons.server.dropwizard.lifecycle;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;

public class ManagedShutdownListenerTest {

  @Test
  public void delegateToOnShutdownOnStop() throws Exception {
    AtomicBoolean shutdownCalled = new AtomicBoolean();
    Managed managedObject = ManagedShutdownListener.onShutdown(() -> shutdownCalled.set(true));
    managedObject.stop();
    assertTrue(shutdownCalled.get());
  }

  @Test
  public void doNotDelegateToOnShutdownOnStart() throws Exception {
    AtomicBoolean shutdownCalled = new AtomicBoolean();
    Managed managedObject = ManagedShutdownListener.onShutdown(() -> shutdownCalled.set(true));
    managedObject.start();
    assertFalse(shutdownCalled.get());
  }
}
