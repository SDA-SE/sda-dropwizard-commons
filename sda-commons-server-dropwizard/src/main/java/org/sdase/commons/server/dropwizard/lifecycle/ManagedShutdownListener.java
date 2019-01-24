package org.sdase.commons.server.dropwizard.lifecycle;

import io.dropwizard.lifecycle.Managed;

/**
 * Functional interface to implement a {@link Managed} as lambda if only {@link Managed#stop()} should be handled.
 */
@FunctionalInterface
public interface ManagedShutdownListener extends Managed {

   /**
    * <p>
    *     To be used with {@linkplain io.dropwizard.setup.Environment#lifecycle() lifecycle}
    *     {@linkplain io.dropwizard.lifecycle.setup.LifecycleEnvironment#manage(Managed) manage} to avoid casting.
    * </p>
    * <ul>
    *    <li><code>environment.lifecycle().manage(onShutdown(myResource::close));</code></li>
    *    <li><code>environment.lifecycle().manage(onShutdown(() -> {resourceA.close(); resourceB.close;}));</code></li>
    * </ul>
    * @param managedShutdownListener the consumer to call on application shutdown after no more requests are accepted
    * @return the given {@code managedShutdownListener}
    */
   static ManagedShutdownListener onShutdown(ManagedShutdownListener managedShutdownListener) {
      return managedShutdownListener;
   }

   /**
    * Stops the object. Called <i>after</i> the application is no longer accepting requests. Will be invoked by
    * {@link #stop()}.
    *
    * @throws Exception if something goes wrong.
    */
   void onShutdown() throws Exception; // NOSONAR


   @Override
   default void start() {
      // nothing to do on startup
   }

   @Override
   default void stop() throws Exception {
      onShutdown();
   }
}
