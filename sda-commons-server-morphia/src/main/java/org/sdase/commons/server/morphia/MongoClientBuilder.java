package org.sdase.commons.server.morphia;

import java.io.IOException;
import java.io.StringReader;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.function.Consumer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import com.google.common.base.Strings;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;

import com.mongodb.MongoException;
import com.mongodb.WriteConcern;

import static java.lang.String.format;

public class MongoClientBuilder {

   private static final Logger LOGGER = LoggerFactory.getLogger(MongoClientBuilder.class);

   private Consumer<MongoClientOptions.Builder> defaultOptions = b -> b.writeConcern(WriteConcern.ACKNOWLEDGED);

   private MongoConfiguration configuration;

   public MongoClientBuilder withConfiguration(MongoConfiguration configuration) {
      this.configuration = configuration;
      return this;
   }

   public MongoClientBuilder addMongoClientOption(Consumer<MongoClientOptions.Builder> clientOption) {
      defaultOptions.andThen(clientOption);
      return this;
   }

   /**
    * build mongo client for for environment
    * @param environment
    * @return
    */
   public MongoClient build(Environment environment) {

      if (!Optional.ofNullable(configuration).isPresent()) {
         throw new IllegalArgumentException("configuration is required");
      } else if (!Optional.ofNullable(defaultOptions).isPresent()) {
         throw new IllegalArgumentException("the defaultOptions are required");
      }

      try {
         LOGGER.info("----------------- Try to connect to MongoDB ---------");
         final MongoClient mongo = createMongoClient();
         LOGGER.info("Mongo database is {}", configuration.getDatabase());
         LOGGER.info("----------------------- Connected ------------------------");
         environment.lifecycle().manage(new Managed() {
            @Override
            public void start() throws Exception {
               // nothing here
            }

            @Override
            public void stop() throws Exception {
               mongo.close();
            }
         });
         return mongo;
      } catch (Exception e) {
         throw new MongoException("Could not configure MongoDB client.", e);
      }
   }

   private MongoClient createMongoClient()
         throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, KeyManagementException {

      MongoClientOptions.Builder builder = MongoClientOptions.builder();

      if (configuration.isSSL()) {
         String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
         TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
         tmf.init(createTruststore(configuration.getCertificate()));

         SSLContext sc = SSLContext.getInstance("TLSv1.2");
         sc.init(null, tmf.getTrustManagers(), new SecureRandom());

         defaultOptions.andThen(b -> b.sslContext(sc)).andThen(b -> b.sslEnabled(true));
      }
      defaultOptions.accept(builder);
      return new MongoClient(buildMongoDbUri(), builder.build());
   }

   private String buildMongoDbUri() {

      StringBuilder builder = new StringBuilder();

      builder.append("mongodb://");

      if (!(Strings.isNullOrEmpty(configuration.getUsername()) || Strings.isNullOrEmpty(configuration.getPassword()))) {
         builder.append(configuration.getUsername()).append(":").append(configuration.getPassword()).append("@");
      }
      builder.append(configuration.getHosts()).append("/").append(configuration.getDatabase());

      if (!Strings.isNullOrEmpty(configuration.getOptions())) {
         builder.append("?").append(configuration.getOptions());
      }

      return builder.toString();

   }

   private KeyStore createTruststore(String certificateAsString) throws IOException, CertificateException, KeyStoreException {
      PEMParser parser = new PEMParser(new StringReader(certificateAsString));
      KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
      int i = 0;
      X509Certificate certificate;

      while ((certificate = parseCert(parser)) != null) {
         ts.setCertificateEntry(format("alias%d", i), certificate);
         i += 1;
      }

      parser.close();
      return ts;
   }

   private X509Certificate parseCert(PEMParser parser) throws IOException, CertificateException {
      X509CertificateHolder certHolder = (X509CertificateHolder) parser.readObject();
      if (certHolder == null) {
         return null;
      }
      return new JcaX509CertificateConverter().getCertificate(certHolder);
   }
}