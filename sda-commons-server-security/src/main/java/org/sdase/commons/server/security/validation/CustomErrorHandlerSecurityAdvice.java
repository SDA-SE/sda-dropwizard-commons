package org.sdase.commons.server.security.validation;

import io.dropwizard.server.AbstractServerFactory;
import io.dropwizard.server.ServerFactory;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.security.exception.InsecureConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 *    Checks that custom error mappers are registered by the JacksonConfigurationBundle. The check is indirectly
 *    performed by checking that the bundle itself is registered. This class checks for the risks identified in the
 *    security guide as:
 * </p>
 * <ul>
 *    <li>"Risiko: Erkennung von vertraulichen Komponenten ... Entfernen von applikations-bezogenen Fehlermeldungen"</li>
 *    <li>"Risiko: Erkennung von vertraulichen Komponenten ... Zentrales Abfangen aller Exceptions"</li>
 * </ul>
 */
public class CustomErrorHandlerSecurityAdvice {

   private static final Logger LOG = LoggerFactory.getLogger(CustomErrorHandlerSecurityAdvice.class);

   private static final String JACKSON_CONFIGURATION_BUNDLE_CLASS = "JacksonConfigurationBundle";
   private static final String JACKSON_CONFIGURATION_BUNDLE_FQN =
         "org.sdase.commons.server.jackson." + JACKSON_CONFIGURATION_BUNDLE_CLASS;
   private Environment environment;
   private AbstractServerFactory abstractServerFactory;

   public CustomErrorHandlerSecurityAdvice(ServerFactory serverFactory, Environment environment) {
      this.environment = environment;
      abstractServerFactory = ServerFactoryUtil.verifyAbstractServerFactory(serverFactory).orElse(null);
   }

   public void applySecureConfiguration() {
      verifyJacksonConfigurationBundleEnabled();
      forceDefaultExceptionMappersAreNotRegistered();
   }

   private void forceDefaultExceptionMappersAreNotRegistered() {
      if (abstractServerFactory.getRegisterDefaultExceptionMappers()) {
         LOG.info("Disabling registerDefaultExceptionHeaders to avoid giving information to possible attackers.");
         abstractServerFactory.setRegisterDefaultExceptionMappers(true);
      }

   }

   private void verifyJacksonConfigurationBundleEnabled() {
      if (environment.jersey().getResourceConfig().getSingletons().stream()
            .map(Object::getClass)
            .map(Class::getName)
            .noneMatch(JACKSON_CONFIGURATION_BUNDLE_FQN::equals)) {
         throw new InsecureConfigurationException(
               "Missing "
                     + JACKSON_CONFIGURATION_BUNDLE_FQN
                     + " from sda-commons-server-jackson. The "
                     + JACKSON_CONFIGURATION_BUNDLE_CLASS
                     + " registers custom error mappers that do not expose server specific error "
                     + "messages/pages.");
      }
   }
}
