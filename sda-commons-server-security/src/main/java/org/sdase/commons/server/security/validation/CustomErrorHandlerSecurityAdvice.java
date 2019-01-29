package org.sdase.commons.server.security.validation;

import io.dropwizard.server.AbstractServerFactory;
import io.dropwizard.server.ServerFactory;
import io.dropwizard.setup.Bootstrap;
import org.sdase.commons.server.security.exception.InsecureConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
   private Bootstrap<?> bootstrap;
   private AbstractServerFactory abstractServerFactory;

   public CustomErrorHandlerSecurityAdvice(ServerFactory serverFactory, Bootstrap<?> bootstrap) {
      this.bootstrap = bootstrap;
      abstractServerFactory = ServerFactoryUtil.verifyAbstractServerFactory(serverFactory).orElse(null);
   }

   public void applySecureConfiguration() {
      verifyJacksonConfigurationBundleEnabled();
      forceDefaultExceptionMappersAreNotRegistered();
   }

   private void forceDefaultExceptionMappersAreNotRegistered() {
      if (abstractServerFactory.getRegisterDefaultExceptionMappers()) {
         LOG.info("Disabling registerDefaultExceptionHeaders to avoid giving information to possible attackers.");
         abstractServerFactory.setRegisterDefaultExceptionMappers(false);
      }

   }

   private void verifyJacksonConfigurationBundleEnabled() {
      List<Class<?>> bundleTypes = extractRegisteredBundleTypes(bootstrap.getClass());
      if (bundleTypes.stream().map(Class::getName).noneMatch(JACKSON_CONFIGURATION_BUNDLE_FQN::equals)) {
         throw new InsecureConfigurationException(
               "Missing "
                     + JACKSON_CONFIGURATION_BUNDLE_FQN
                     + " from sda-commons-server-jackson. The "
                     + JACKSON_CONFIGURATION_BUNDLE_CLASS
                     + " registers custom error mappers that do not expose server specific error "
                     + "messages/pages.");
      }
   }

   private List<Class<?>> extractRegisteredBundleTypes(Class<?> classOrSuperClass) {
      List<Class<?>> registeredBundles = new ArrayList<>();
      try {
         List<Field> bundleFields = Stream.of(classOrSuperClass.getDeclaredFields())
               .filter(f -> "bundles".equals(f.getName()) || "configuredBundles".equals(f.getName()))
               .collect(Collectors.toList());
         for (Field bundleField : bundleFields) {
            bundleField.setAccessible(true);
            //noinspection unchecked
            List<Object> bundles = (List<Object>) bundleField.get(bootstrap);
            registeredBundles.addAll(bundles.stream().map(Object::getClass).collect(Collectors.toList()));
         }
         if (!Object.class.equals(classOrSuperClass)) {
            registeredBundles.addAll(extractRegisteredBundleTypes(classOrSuperClass.getSuperclass()));
         }
      } catch (Exception e) {
         throw new IllegalStateException(
               "Could not verify registered bundles. Added bundles are checked using reflection.", e);
      }
      return registeredBundles;
   }
}
