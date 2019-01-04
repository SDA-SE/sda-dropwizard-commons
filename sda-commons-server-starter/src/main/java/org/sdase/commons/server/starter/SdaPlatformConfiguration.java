package org.sdase.commons.server.starter;

import io.dropwizard.Configuration;
import org.sdase.commons.server.auth.config.AuthConfig;
import org.sdase.commons.server.cors.CorsConfiguration;

/**
 * Default configuration for the {@link SdaPlatformBundle}.
 */
public class SdaPlatformConfiguration extends Configuration {

   /**
    * Configuration of authentication.
    */
   private AuthConfig auth = new AuthConfig();

   /**
    * Configuration of the CORS filter.
    */
   private CorsConfiguration cors = new CorsConfiguration();

   public AuthConfig getAuth() {
      return auth;
   }

   public SdaPlatformConfiguration setAuth(AuthConfig auth) {
      this.auth = auth;
      return this;
   }

   public CorsConfiguration getCors() {
      return cors;
   }

   public SdaPlatformConfiguration setCors(CorsConfiguration cors) {
      this.cors = cors;
      return this;
   }

}
