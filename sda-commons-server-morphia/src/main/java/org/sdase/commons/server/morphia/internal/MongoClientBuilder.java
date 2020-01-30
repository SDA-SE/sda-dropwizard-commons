package org.sdase.commons.server.morphia.internal;

import static org.sdase.commons.server.dropwizard.lifecycle.ManagedShutdownListener.onShutdown;
import static org.sdase.commons.server.morphia.internal.ConnectionStringUtil.createConnectionString;
import static org.sdase.commons.server.morphia.internal.SslUtil.createTruststoreFromPemKey;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import io.dropwizard.setup.Environment;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLContext;
import org.apache.commons.lang3.StringUtils;
import org.sdase.commons.server.morphia.MongoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoClientBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(MongoClientBuilder.class);

  private static final MongoClientOptions.Builder DEFAULT_OPTIONS =
      MongoClientOptions.builder().writeConcern(WriteConcern.ACKNOWLEDGED);

  private final MongoConfiguration configuration;
  private final MongoClientOptions.Builder mongoClientOptionsBuilder;

  public MongoClientBuilder(MongoConfiguration configuration) {
    this(configuration, MongoClientOptions.builder(DEFAULT_OPTIONS.build()));
  }

  MongoClientBuilder(
      MongoConfiguration configuration, MongoClientOptions.Builder mongoClientOptionsBuilder) {
    this.configuration = configuration;
    this.mongoClientOptionsBuilder = mongoClientOptionsBuilder;
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

  private MongoClient createMongoClient()
      throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException,
          KeyManagementException {

    if (configuration.isUseSsl()) {
      mongoClientOptionsBuilder.sslEnabled(true);
      SSLContext sslContext = createSslContextIfAnyCertificatesAreConfigured();
      if (sslContext != null) {
        mongoClientOptionsBuilder.sslContext(sslContext);
      }
    }
    return new MongoClient(
        new MongoClientURI(createConnectionString(configuration), mongoClientOptionsBuilder));
  }

  private SSLContext createSslContextIfAnyCertificatesAreConfigured()
      throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException,
          KeyManagementException {
    String caCertificate = configuration.getCaCertificate();
    if (StringUtils.isNotBlank(caCertificate)) {
      KeyStore truststoreFromPemKey = createKeyStoreFromCaCertificate(caCertificate);
      return SslUtil.createSslContext(truststoreFromPemKey);
    }
    return null;
  }

  private KeyStore createKeyStoreFromCaCertificate(String caCertificate)
      throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
    KeyStore truststoreFromPemKey = null;
    if (StringUtils.isNotBlank(caCertificate)) {
      truststoreFromPemKey = createTruststoreFromPemKey(caCertificate);
    }
    return truststoreFromPemKey;
  }
}
