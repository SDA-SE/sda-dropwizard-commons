package org.sdase.commons.starter;

import io.dropwizard.Configuration;
import org.sdase.commons.server.auth.config.AuthConfig;
import org.sdase.commons.server.cors.CorsConfiguration;
import org.sdase.commons.server.opa.config.OpaConfig;

/** Default configuration for the {@link SdaPlatformBundle}. */
public class SdaPlatformConfiguration extends Configuration {

  /** Configuration of authentication. */
  private AuthConfig auth = new AuthConfig();

  /** Configuration of the open policy agent. */
  private OpaConfig opa = new OpaConfig();

  /** Configuration of the CORS filter. */
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

  public OpaConfig getOpa() {
    return opa;
  }

  public SdaPlatformConfiguration setOpa(OpaConfig opa) {
    this.opa = opa;
    return this;
  }
}
