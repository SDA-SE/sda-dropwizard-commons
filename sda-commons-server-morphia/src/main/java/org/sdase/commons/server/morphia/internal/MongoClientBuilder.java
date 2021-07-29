package org.sdase.commons.server.morphia.internal;

import static java.util.Arrays.asList;
import static org.sdase.commons.server.dropwizard.lifecycle.ManagedShutdownListener.onShutdown;
import static org.sdase.commons.server.morphia.internal.ConnectionStringUtil.createConnectionString;

import com.mongodb.*;
import io.dropwizard.setup.Environment;
import io.opentracing.Tracer;
import io.opentracing.contrib.mongo.common.SpanDecorator;
import io.opentracing.contrib.mongo.common.TracingCommandListener;
import io.opentracing.util.GlobalTracer;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import org.apache.commons.lang3.StringUtils;
import org.sdase.commons.server.morphia.MongoConfiguration;
import org.sdase.commons.shared.certificates.ca.ssl.SslUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoClientBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(MongoClientBuilder.class);

  private static final MongoClientOptions.Builder DEFAULT_OPTIONS =
      MongoClientOptions.builder().writeConcern(WriteConcern.ACKNOWLEDGED);

  private final MongoConfiguration configuration;
  private final MongoClientOptions.Builder mongoClientOptionsBuilder;
  private Tracer tracer;
  private SSLContext sslContext;

  public MongoClientBuilder(MongoConfiguration configuration) {
    this(configuration, MongoClientOptions.builder(DEFAULT_OPTIONS.build()));
  }

  MongoClientBuilder(
      MongoConfiguration configuration, MongoClientOptions.Builder mongoClientOptionsBuilder) {
    this.configuration = configuration;
    this.mongoClientOptionsBuilder = mongoClientOptionsBuilder;
  }

  public MongoClientBuilder withTracer(Tracer tracer) {
    this.tracer = tracer;
    return this;
  }

  public MongoClientBuilder withSSlContext(SSLContext sslContext) {
    this.sslContext = sslContext;
    return this;
  }

  /**
   * build mongo client for environment
   *
   * @param environment the dropwizard environment of the current application
   * @return a {@link MongoClient} that can access the configured database
   */
  public MongoClient build(Environment environment) {

    if (configuration == null) {
      throw new IllegalArgumentException("configuration is required");
    }
    try {
      LOGGER.info("Connecting to MongoDB at '{}'", configuration.getHosts());
      final MongoClient mongoClient = createMongoClient();
      environment.lifecycle().manage(onShutdown(mongoClient::close));
      LOGGER.info("Connected to MongoDB at '{}'", configuration.getHosts());
      return mongoClient;
    } catch (Exception e) {
      throw new MongoException("Could not configure MongoDB client.", e);
    }
  }

  private MongoClient createMongoClient() {
    if (configuration.isUseSsl()) {
      mongoClientOptionsBuilder.sslEnabled(true);
      // use sslContext created with env variable by default
      if (StringUtils.isNotBlank(configuration.getCaCertificate())) {
        LOGGER.info("Overriding ssl config from env variable");
        sslContext = createSslContextIfAnyCertificatesAreConfigured();
      }
      if (sslContext != null) {
        mongoClientOptionsBuilder.sslContext(sslContext);
      }
    }

    // Initialize a tracer that traces all calls to the MongoDB server.
    Tracer currentTracer = tracer == null ? GlobalTracer.get() : tracer;
    TracingCommandListener listener =
        new TracingCommandListener.Builder(currentTracer)
            .withSpanDecorators(asList(SpanDecorator.DEFAULT, new NoStatementSpanDecorator()))
            .build();
    mongoClientOptionsBuilder.addCommandListener(listener);

    return new MongoClient(
        new MongoClientURI(createConnectionString(configuration), mongoClientOptionsBuilder));
  }

  private SSLContext createSslContextIfAnyCertificatesAreConfigured() {
    String caCertificate = configuration.getCaCertificate();
    KeyStore truststoreFromPemKey = SslUtil.createTruststoreFromPemKey(caCertificate);
    return SslUtil.createSslContext(truststoreFromPemKey);
  }
}
