package org.sdase.commons.server.security.validation;

import io.dropwizard.server.AbstractServerFactory;
import io.dropwizard.server.ServerFactory;
import org.sdase.commons.server.security.exception.InsecureConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;

/**
 * <p>
 *    Checks that secure defaults of used {@link AbstractServerFactory} instances are not modified and overwrites
 *    insecure defaults. This class checks for the risks identified in the security guide as:
 * </p>
 * <ul>
 *    <li>"Risiko: Root-Start"</li>
 *    <li>"Risiko: Ausnutzung von HTTP-Methoden"</li>
 * </ul>
 */
public class ServerFactorySecurityAdvice {

   private static final Logger LOG = LoggerFactory.getLogger(ServerFactorySecurityAdvice.class);

   private static final Set<String> SECURE_HTTP_METHODS = new HashSet<>(Arrays.asList(
         HttpMethod.OPTIONS,
         HttpMethod.HEAD,
         HttpMethod.GET,
         HttpMethod.POST,
         HttpMethod.PUT,
         "PATCH",
         HttpMethod.DELETE
   ));

   private AbstractServerFactory abstractServerFactory;

   public ServerFactorySecurityAdvice(ServerFactory serverFactory) {
      if (serverFactory instanceof AbstractServerFactory) {
         abstractServerFactory = (AbstractServerFactory) serverFactory;
      }
      else if (serverFactory == null) {
         LOG.error("Unable to apply secure server config. Expecting an AbstractServerFactory but found null");
      }
      else {
         LOG.error("Unable to apply secure server config. Expecting an AbstractServerFactory but found a {}",
               serverFactory.getClass());
      }
   }

   public void applySecureConfiguration() {
      if (abstractServerFactory == null) {
         return;
      }
      forceAllowedMethods();
      forceNotStartAsRoot();
   }

   private void forceNotStartAsRoot() {
      if (TRUE.equals(abstractServerFactory.getStartsAsRoot())) {
         throw new InsecureConfigurationException(
               "Configuration allows server to start as root: server.startAsRoot is "
                     + abstractServerFactory.getStartsAsRoot());
      }
      // no explicit config (default) or good config, anyway, we set it
      abstractServerFactory.setStartsAsRoot(false);
   }

   private void forceAllowedMethods() {
      Set<String> filteredAllowedMethods = abstractServerFactory.getAllowedMethods().stream()
            .filter(Objects::nonNull)
            .filter(m -> !m.trim().isEmpty())
            .collect(Collectors.toSet());

      Set<String> inSecureAllowedMethods = new HashSet<>();
      for (String allowedMethod : filteredAllowedMethods) {
         if (!SECURE_HTTP_METHODS.contains(allowedMethod.trim().toUpperCase())) {
            inSecureAllowedMethods.add(allowedMethod);
         }
      }

      if (!inSecureAllowedMethods.isEmpty()) {
         throw new InsecureConfigurationException(
               "Configuration server.allowedMethods contains insecure methods "
                     + String.join(", ", inSecureAllowedMethods));
      }
   }

}
