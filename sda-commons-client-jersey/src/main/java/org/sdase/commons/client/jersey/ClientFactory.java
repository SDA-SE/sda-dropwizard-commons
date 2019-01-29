package org.sdase.commons.client.jersey;

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.setup.Environment;
import org.sdase.commons.client.jersey.builder.ExternalClientBuilder;
import org.sdase.commons.client.jersey.builder.PlatformClientBuilder;

/**
 * A {@code ClientFactory} creates Http clients to access services within the SDA Platform or external services.
 */
public class ClientFactory {

   private JerseyClientBuilder clientBuilderWithGzipCompression;
   private JerseyClientBuilder clientBuilderWithoutGzipCompression;

   private String consumerToken;

   ClientFactory(Environment environment, String consumerToken) {
      JerseyClientConfiguration configurationWithGzip = new JerseyClientConfiguration();
      this.clientBuilderWithGzipCompression = new JerseyClientBuilder(environment).using(configurationWithGzip);
      JerseyClientConfiguration configurationWithoutGzip = new JerseyClientConfiguration();
      configurationWithoutGzip.setGzipEnabled(false);
      configurationWithoutGzip.setGzipEnabledForRequests(false);
      this.clientBuilderWithoutGzipCompression = new JerseyClientBuilder(environment).using(configurationWithoutGzip);
      this.consumerToken = consumerToken;
   }

   /**
    * <p>
    *    Starts creation of a client that calls APIs within the SDA SE Platform. This clients automatically send a
    *    {@code Trace-Token} from the incoming request or a new {@code Trace-Token} to the API resources and can
    *    optionally send a {@code Consumer-Token} or pass through the {@code Authorization} header from the incoming
    *    request.
    * </p>
    * <p>
    *    The client is using gzip compression.
    * </p>
    *
    * @return a builder to configure the client
    */
   public PlatformClientBuilder platformClient() {
      return platformClient(false);
   }

   /**
    * Starts creation of a client that calls APIs within the SDA SE Platform. This clients automatically send a
    * {@code Trace-Token} from the incoming request or a new {@code Trace-Token} to the API resources and can optionally
    * send a {@code Consumer-Token} or pass through the {@code Authorization} header from the incoming request.
    *
    * @param disableGzipCompression if gzip compression of requests should be disabled. This may be needed if the server
    *                               can not communicate with gzip enabled
    * @return a builder to configure the client
    */
   public PlatformClientBuilder platformClient(boolean disableGzipCompression) {
      return new PlatformClientBuilder(findJerseyClientBuilder(disableGzipCompression), consumerToken);
   }

   /**
    * <p>
    *    Starts creation of a client that calls APIs outside of the SDA SE Platform. This clients does no header magic.
    * </p>
    * <p>
    *    The client is using gzip compression.
    * </p>
    *
    * @return a builder to configure the client
    */
   public ExternalClientBuilder externalClient() {
      return externalClient(false);
   }

   /**
    * Starts creation of a client that calls APIs outside of the SDA SE Platform. This clients does no header magic.
    *
    * @param disableGzipCompression if gzip compression of requests should be disabled. This may be needed if the server
    *                               can not communicate with gzip enabled
    * @return a builder to configure the client
    */
   public ExternalClientBuilder externalClient(boolean disableGzipCompression) {
      return new ExternalClientBuilder(findJerseyClientBuilder(disableGzipCompression));
   }

   private JerseyClientBuilder findJerseyClientBuilder(boolean disableGzipCompression) {
      return disableGzipCompression ? clientBuilderWithoutGzipCompression : clientBuilderWithGzipCompression;
   }

}
