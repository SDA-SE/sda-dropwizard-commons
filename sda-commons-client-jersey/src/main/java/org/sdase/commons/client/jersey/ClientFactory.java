package org.sdase.commons.client.jersey;

import io.dropwizard.client.JerseyClientBuilder;
import org.sdase.commons.client.jersey.builder.ExternalClientBuilder;
import org.sdase.commons.client.jersey.builder.PlatformClientBuilder;

/**
 * A {@code ClientFactory} creates Http clients to access services within the SDA Platform or external services.
 */
public class ClientFactory {

   private JerseyClientBuilder clientBuilder;

   private String consumerToken;

   ClientFactory(JerseyClientBuilder clientBuilder, String consumerToken) {
      this.clientBuilder = clientBuilder;
      this.consumerToken = consumerToken;
   }

   /**
    * Starts creation of a client that calls APIs within the SDA SE Platform. This clients automatically send a
    * {@code Trace-Token} from the incoming request or a new {@code Trace-Token} to the API resources and can optionally
    * send a {@code Consumer-Token} or pass through the {@code Authorization} header from the incoming request.
    *
    * @return a builder to configure the client
    */
   public PlatformClientBuilder platformClient() {
      return new PlatformClientBuilder(clientBuilder, consumerToken);
   }

   /**
    * Starts creation of a client that calls APIs outside of the SDA SE Platform. This clients does no header magic.
    *
    * @return a builder to configure the client
    */
   public ExternalClientBuilder externalClient() {
      return new ExternalClientBuilder(clientBuilder);
   }

}
