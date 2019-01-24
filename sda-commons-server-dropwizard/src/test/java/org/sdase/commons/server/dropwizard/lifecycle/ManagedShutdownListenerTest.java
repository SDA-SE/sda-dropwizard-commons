package org.sdase.commons.server.dropwizard.lifecycle;

import io.dropwizard.lifecycle.Managed;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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