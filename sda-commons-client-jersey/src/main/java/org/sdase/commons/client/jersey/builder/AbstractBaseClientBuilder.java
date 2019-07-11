package org.sdase.commons.client.jersey.builder;

import io.dropwizard.client.JerseyClientBuilder;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestFilter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder that provides options that are common for all types of clients.
 *
 * @param <T> the type of the subclass
 */
abstract class AbstractBaseClientBuilder<T extends AbstractBaseClientBuilder> {

   private static final Logger LOG = LoggerFactory.getLogger(AbstractBaseClientBuilder.class);

   /**
    * The default timeout to wait for data in an established connection. 2 seconds is used as a trade between "fail
    * fast" and "better return late than no result". The timeout may be changed according to the use case considering
    * how long a user is willing to wait and how long backend operations need.
    */
   private static final int DEFAULT_READ_TIMEOUT_MS = 2_000;

   /**
    * The default timeout to wait until a connection is established. 500ms should be suitable for all communication in
    * the platform. Clients that request information from external services may extend this timeout if foreign services
    * are usually slow.
    */
   private static final int DEFAULT_CONNECTION_TIMEOUT_MS = 500;


   /**
    * As default, the client will follow redirects so that redirect status codes are automatically resolved by the client.
    */
   private static final boolean DEFAULT_FOLLOW_REDIRECTS = true;

   private JerseyClientBuilder jerseyClientBuilder;

   private List<ClientRequestFilter> filters;

   private int connectionTimeoutMillis;

   private int readTimeoutMillis;

   private boolean followRedirects;

   AbstractBaseClientBuilder(JerseyClientBuilder jerseyClientBuilder) {
      this.jerseyClientBuilder = jerseyClientBuilder;
      this.filters = new ArrayList<>();
      this.readTimeoutMillis = DEFAULT_READ_TIMEOUT_MS;
      this.connectionTimeoutMillis = DEFAULT_CONNECTION_TIMEOUT_MS;
      this.followRedirects = DEFAULT_FOLLOW_REDIRECTS;
   }

   /**
    * Adds a request filter to the client.
    *
    * @param clientRequestFilter the filter to add
    *
    * @return this builder instance
    */
   public T addFilter(ClientRequestFilter clientRequestFilter) {
      this.filters.add(clientRequestFilter);
      //noinspection unchecked
      return (T) this;
   }

   /**
    * <p>
    *    Sets the connection timeout for the clients that are built with this instance. The connection timeout is the
    *    amount of time to wait until the connection to the server is established. The default is
    *    {@value #DEFAULT_CONNECTION_TIMEOUT_MS}ms.
    * </p>
    * <p>
    *    If the connection timeout is overdue a {@link javax.ws.rs.ProcessingException} wrapping a
    *    {@link org.apache.http.conn.ConnectTimeoutException} is thrown by the client.
    * </p>
    *
    * @param connectionTimeout the time to wait until a connection to the remote service is established
    * @return this builder instance
    */
   public T withConnectionTimeout(Duration connectionTimeout) {
      this.connectionTimeoutMillis = (int) connectionTimeout.toMillis();
      //noinspection unchecked
      return (T) this;
   }

   /**
    * <p>
    *    Sets the read timeout for the clients that are built with this instance. The read timeout is the timeout to
    *    wait for data in an established connection. Usually this timeout is violated when the client has sent the
    *    request and is waiting for the first byte of the response while the server is doing calculations, accessing a
    *    database or delegating to other services. The default is {@value #DEFAULT_READ_TIMEOUT_MS}ms. The read timeout
    *    should be set wisely according to the use case considering how long a user is willing to wait and how long
    *    backend operations need.
    * </p>
    * <p>
    *    If the connection timeout is overdue a {@link javax.ws.rs.ProcessingException} wrapping a
    *    {@link java.net.SocketTimeoutException} is thrown by the client.
    * </p>
    *
    * @param readTimeout the time to wait for content in an established connection
    * @return this builder instance
    */
   public T withReadTimeout(Duration readTimeout) {
      this.readTimeoutMillis = (int) readTimeout.toMillis();
      //noinspection unchecked
      return (T) this;
   }

   /**
    * <p>
    *   Set this client to not follow redirects and therewith automatically resolve 3xx status codes
    * </p>
    * @return this builder instance
    */
   public T disableFollowRedirects() {
      this.followRedirects = false;
      return (T) this;
   }

   /**
    * Builds a generic client that can be used for Http requests.
    *
    * @param name the name of the client is used for metrics and thread names
    * @return the client instance
    */
   public Client buildGenericClient(String name) {
      Client client = jerseyClientBuilder.build(name);
      filters.forEach(client::register);
      client.property(ClientProperties.FOLLOW_REDIRECTS, followRedirects);
      client.property(ClientProperties.CONNECT_TIMEOUT, connectionTimeoutMillis);
      client.property(ClientProperties.READ_TIMEOUT, readTimeoutMillis);
      registerMultiPartIfAvailable(client);
      return client;
   }

   /**
    * Creates a client proxy implementation for accessing another service.
    *
    * @param apiInterface the interface that declares the API using JAX-RS annotations.
    * @param <A> the type of the api
    * @return a builder to define the root path of the API for the proxy that is build
    */
   public <A> ApiClientBuilder<A> api(Class<A> apiInterface) {
      return api(apiInterface, apiInterface.getSimpleName());
   }

   /**
    * Creates a client proxy implementation for accessing another service.
    * Allows to set a custom name if required, e.g. if you have multiple clients
    * generated from the same interface.
    *
    * @param apiInterface
    *           the interface that declares the API using JAX-RS annotations.
    * @param customName
    *           the custom name to use for the client. The name is used for the
    *           executor service and metrics. Names have to be unique.
    * @param <A>
    *           the type of the api
    * @return a builder to define the root path of the API for the proxy that is
    *         build
    */
   public <A> ApiClientBuilder<A> api(Class<A> apiInterface, String customName) {
      return new ApiClientBuilder<>(apiInterface, buildGenericClient(customName));
   }

   private void registerMultiPartIfAvailable(Client client) {
      try {
         ClassLoader classLoader = getClass().getClassLoader();
         Class<?> multiPartFeature = classLoader.loadClass("org.glassfish.jersey.media.multipart.MultiPartFeature");
         if (multiPartFeature != null) {
            LOG.info("Registering MultiPartFeature for client.");
            client.register(multiPartFeature);
         }
      } catch (ClassNotFoundException e) {
         LOG.info("Not registering MultiPartFeature for client: Class is not available.");
      } catch (Exception e) {
         LOG.warn("Failed to register MultiPartFeature for client.");
      }
   }

}
