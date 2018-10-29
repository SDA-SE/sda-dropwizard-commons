package com.sdase.commons.server.weld;

import com.sdase.commons.server.weld.internal.WeldSupport;
import io.dropwizard.Application;
import org.jboss.weld.environment.se.WeldContainer;

/**
 * <p>
 * Start a Dropwizard application with Weld support.
 * </p>
 * <p>
 * This is used to create the Application class inside the Weld Context, so that
 * the Application, or instances produced by the Application, can be injected.
 * </p>
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 *     <code>
 *     public static void main(final String[]args) throws Exception {
 *         DropwizardWeldHelper.run(Application.class, args);
 *     }
 *     </code>
 * </pre>
 */
public class DropwizardWeldHelper {
   // We are rethrowing a generic exception, ignore the warning
   public static <C extends Application> void run(Class<C> applicationClass, String... arguments) throws Exception { // NOSONAR
      WeldSupport.initializeCDIProviderIfRequired();

      try (WeldContainer weldContainer = WeldSupport.createWeldContainer()) {
         weldContainer.select(applicationClass).get().run(arguments);
      }
   }

   private DropwizardWeldHelper() {
      // No public constructor
   }
}
