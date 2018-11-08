package org.sdase.commons.server.weld.internal;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.environment.se.WeldSEProvider;

import javax.enterprise.inject.spi.CDI;

public class WeldSupport {
   private static boolean isCDIProviderInitialized = false;

   public static void initializeCDIProviderIfRequired() {
      if (!isCDIProviderInitialized) {
         // Register Weld provider to allow access via CDI.current(), this is only possible once per application!
         CDI.setCDIProvider(new WeldSEProvider());
         isCDIProviderInitialized = true;
      }
   }

   public static WeldContainer createWeldContainer() {
      return new Weld().enableDiscovery().initialize();
   }

   private WeldSupport() {
      // No public constructor
   }
}
