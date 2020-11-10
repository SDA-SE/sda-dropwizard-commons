package org.sdase.commons.client.jersey.builder;

import static org.sdase.commons.server.opentracing.client.ClientTracingUtil.registerTracing;

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.opentracing.Tracer;
import java.net.ProxySelector;
import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestFilter;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class InternalJerseyClientFactory {

  private static final Logger LOG = LoggerFactory.getLogger(InternalJerseyClientFactory.class);

  private JerseyClientBuilder jerseyClientBuilder;
  private Tracer tracer;

  InternalJerseyClientFactory(JerseyClientBuilder jerseyClientBuilder, Tracer tracer) {
    this.jerseyClientBuilder = jerseyClientBuilder;
    this.tracer = tracer;
  }

  Client createClient(
      String name,
      JerseyClientConfiguration clientConfiguration,
      List<ClientRequestFilter> filters,
      boolean followRedirects) {
    // a specific proxy configuration always overrides the system proxy
    if (clientConfiguration.getProxyConfiguration() == null) {
      // register a route planner that uses the default proxy variables (e.g. http.proxyHost)
      this.jerseyClientBuilder.using(new SystemDefaultRoutePlanner(ProxySelector.getDefault()));
    }
    Client client = jerseyClientBuilder.using(clientConfiguration).build(name);
    filters.forEach(client::register);
    client.property(ClientProperties.FOLLOW_REDIRECTS, followRedirects);
    registerMultiPartIfAvailable(client);
    registerTracing(client, tracer);
    return client;
  }

  private void registerMultiPartIfAvailable(Client client) {
    try {
      ClassLoader classLoader = this.getClass().getClassLoader();
      Class<?> multiPartFeature =
          classLoader.loadClass("org.glassfish.jersey.media.multipart.MultiPartFeature");
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
