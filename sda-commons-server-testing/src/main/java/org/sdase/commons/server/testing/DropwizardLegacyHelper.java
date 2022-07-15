package org.sdase.commons.server.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jersey.jackson.JacksonFeature;
import javax.ws.rs.client.Client;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClientBuilder;

public class DropwizardLegacyHelper {

  private static final int DEFAULT_CONNECT_TIMEOUT_MS = 1000;
  private static final int DEFAULT_READ_TIMEOUT_MS = 5000;

  private DropwizardLegacyHelper() {
    // prevent instantiation
  }

  /**
   * Provides legacy Jersey Client that was changed when updating from Dropwizard 2.1.0 to 2.1.1.
   *
   * @see <a
   *     href="https://www.dropwizard.io/en/latest/manual/upgrade-notes/upgrade-notes-2_1_x.html#modification-of-the-client-in-dropwizardappextension">Upgrade
   *     notes</a>
   */
  public static Client client(ObjectMapper objectMapper) {
    return new JerseyClientBuilder()
        .register(new JacksonFeature(objectMapper))
        .property(ClientProperties.CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT_MS)
        .property(ClientProperties.READ_TIMEOUT, DEFAULT_READ_TIMEOUT_MS)
        // .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true) // Disabled for Java 16+
        .build();
  }
}
