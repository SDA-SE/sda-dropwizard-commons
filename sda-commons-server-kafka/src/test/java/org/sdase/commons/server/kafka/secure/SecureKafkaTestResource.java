package org.sdase.commons.server.kafka.secure;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.ScramCredentialInfo;
import org.apache.kafka.clients.admin.ScramMechanism;
import org.apache.kafka.clients.admin.UserScramCredentialUpsertion;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.network.ListenerName;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.test.KafkaClusterTestKit;
import org.apache.kafka.common.test.TestKitNodes;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Docker-free, single-node KRaft broker for security integration tests (Kafka 4.4.0).
 *
 * <p>Clients connect to the authenticated EXTERNAL listener. INTERNAL and CONTROLLER are
 * loopback-only PLAINTEXT listeners used for inter-broker operations and SCRAM provisioning. This
 * is a TEST-ONLY fixture, not a production broker configuration. Avoid concurrent use of the same
 * fixture across tests and never expose the process's loopback ports externally.
 *
 * <p>The KafkaClusterTestKit API is an internal Apache Kafka testing API. Keep the Kafka
 * runtime/testkit versions aligned and re-run the security integration tests after upgrades.
 */
public final class SecureKafkaTestResource implements BeforeAllCallback, AfterAllCallback {
  private static final Duration TIMEOUT = Duration.ofSeconds(30);
  private static final String EXTERNAL_LISTENER = "EXTERNAL";
  private static final String INTERNAL_LISTENER = "INTERNAL";
  private static final String CONTROLLER_LISTENER = "CONTROLLER";

  public enum Mode {
    SSL("SSL"),
    SASL_PLAIN("SASL_PLAINTEXT"),
    SASL_SCRAM_512("SASL_PLAINTEXT");

    private final String protocol;

    Mode(String protocol) {
      this.protocol = protocol;
    }
  }

  private final Mode mode;
  private final Map<String, String> brokerProperties = new HashMap<>();
  private volatile KafkaClusterTestKit cluster;
  private String username;
  private String password;
  private Path serverKeystore;
  private Path serverTruststore;
  private String serverStorePassword;
  private String serverKeyPassword;
  private Path clientTruststore;
  private Path clientKeystore;
  private String clientStorePassword;
  private String clientKeyPassword;
  private String clientAuth = "required";

  private SecureKafkaTestResource(Mode mode) {
    this.mode = mode;
  }

  public static SecureKafkaTestResource ssl() {
    return new SecureKafkaTestResource(Mode.SSL);
  }

  public static SecureKafkaTestResource saslPlain(String username, String password) {
    SecureKafkaTestResource resource = new SecureKafkaTestResource(Mode.SASL_PLAIN);
    resource.username = safeCredential(username);
    resource.password = safeCredential(password);
    return resource;
  }

  public static SecureKafkaTestResource scramSha512(String username, String password) {
    SecureKafkaTestResource resource = new SecureKafkaTestResource(Mode.SASL_SCRAM_512);
    resource.username = safeCredential(username);
    resource.password = safeCredential(password);
    return resource;
  }

  public synchronized SecureKafkaTestResource withBrokerProperty(String key, String value) {
    requireNotStarted();
    // Allow only ordinary test-specific broker settings; protect the secure listener layout.
    if (!List.of(
            "offsets.topic.num.partitions",
            "offsets.topic.replication.factor",
            "group.initial.rebalance.delay.ms",
            "transaction.state.log.replication.factor",
            "transaction.state.log.min.isr",
            "auto.create.topics.enable")
        .contains(key)) {
      throw new IllegalArgumentException("Unsupported test broker property: " + key);
    }
    brokerProperties.put(key, Objects.requireNonNull(value));
    return this;
  }

  /**
   * Sets existing JKS stores, usually the fixtures already present in the SDA Kafka tests. Client
   * keystore may be null for the optional-certificate (requested) SSL scenario.
   */
  public synchronized SecureKafkaTestResource withTlsStores(
      String serverKeystore,
      String serverTruststore,
      String serverStorePassword,
      String serverKeyPassword,
      String clientTruststore,
      String clientKeystore,
      String clientStorePassword,
      String clientKeyPassword) {
    requireNotStarted();
    if (mode != Mode.SSL) {
      throw new IllegalStateException("TLS stores apply only to SSL mode");
    }
    this.serverKeystore = existingFile(serverKeystore);
    this.serverTruststore = existingFile(serverTruststore);
    this.serverStorePassword = Objects.requireNonNull(serverStorePassword);
    this.serverKeyPassword = Objects.requireNonNull(serverKeyPassword);
    this.clientTruststore = existingFile(clientTruststore);
    this.clientKeystore = clientKeystore == null ? null : existingFile(clientKeystore);
    this.clientStorePassword = Objects.requireNonNull(clientStorePassword);
    this.clientKeyPassword = clientKeyPassword;
    return this;
  }

  public synchronized SecureKafkaTestResource withClientAuth(String clientAuth) {
    requireNotStarted();
    if (!"requested".equals(clientAuth) && !"required".equals(clientAuth)) {
      throw new IllegalArgumentException("ssl.client.auth must be requested or required");
    }
    this.clientAuth = clientAuth;
    return this;
  }

  @Override
  public synchronized void beforeAll(ExtensionContext context) {
    if (cluster != null) {
      return;
    }
    if (mode == Mode.SSL) {
      validateTlsStores();
    }

    // Node-level PLAINTEXT is intentional: the actual EXTERNAL listener is secured using
    // the explicit listener.security.protocol.map below. It also avoids the testkit's
    // all-listeners SSL restriction and its JVM-global JAAS configuration.
    TestKitNodes nodes =
        new TestKitNodes.Builder()
            .setCombined(true)
            .setNumBrokerNodes(1)
            .setNumControllerNodes(1)
            .setBrokerListenerName(ListenerName.normalised(EXTERNAL_LISTENER))
            .build();

    KafkaClusterTestKit.Builder builder =
        new KafkaClusterTestKit.Builder(nodes).setDeleteOnClose(false);
    builder.setConfigProp(
        "listeners", "EXTERNAL://localhost:0,INTERNAL://localhost:0,CONTROLLER://localhost:0");
    builder.setConfigProp(
        "listener.security.protocol.map",
        "EXTERNAL:" + mode.protocol + ",INTERNAL:PLAINTEXT,CONTROLLER:PLAINTEXT");
    builder.setConfigProp("inter.broker.listener.name", INTERNAL_LISTENER);
    builder.setConfigProp("controller.listener.names", CONTROLLER_LISTENER);
    builder.setConfigProp("offsets.topic.replication.factor", "1");
    builder.setConfigProp("transaction.state.log.replication.factor", "1");
    builder.setConfigProp("transaction.state.log.min.isr", "1");
    builder.setConfigProp("group.initial.rebalance.delay.ms", "0");
    brokerProperties.forEach(builder::setConfigProp);
    configureSecurity(builder);

    KafkaClusterTestKit candidate = null;
    try {
      candidate = builder.build();
      candidate.format();
      candidate.startup();
      candidate.waitForReadyBrokers();
      if (mode == Mode.SASL_SCRAM_512) {
        provisionScramUser(candidate);
      }
      // Verify via the *secure* listener, not the internal bootstrap connection.
      try (Admin admin = Admin.create(clientProperties(candidate.bootstrapServers()))) {
        admin.describeCluster().clusterId().get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
      }
      cluster = candidate;
    } catch (Exception e) {
      if (candidate != null) {
        try {
          candidate.close();
        } catch (Exception cleanupFailure) {
          e.addSuppressed(cleanupFailure);
        }
      }
      throw new IllegalStateException("Could not start embedded secured KRaft broker", e);
    }
  }

  private void configureSecurity(KafkaClusterTestKit.Builder builder) {
    if (mode == Mode.SSL) {
      builder.setConfigProp(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, serverKeystore.toString());
      builder.setConfigProp(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, serverStorePassword);
      builder.setConfigProp(SslConfigs.SSL_KEY_PASSWORD_CONFIG, serverKeyPassword);
      builder.setConfigProp(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, serverTruststore.toString());
      builder.setConfigProp(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, serverStorePassword);
      builder.setConfigProp("ssl.client.auth", clientAuth);
      return;
    }

    String mechanism = mode == Mode.SASL_PLAIN ? "PLAIN" : "SCRAM-SHA-512";
    String loginModule =
        mode == Mode.SASL_PLAIN
            ? "org.apache.kafka.common.security.plain.PlainLoginModule"
            : "org.apache.kafka.common.security.scram.ScramLoginModule";
    String jaas =
        loginModule
            + " required username=\""
            + username
            + "\" password=\""
            + password
            + "\""
            + (mode == Mode.SASL_PLAIN ? " user_" + username + "=\"" + password + "\"" : "")
            + ";";
    builder.setConfigProp("sasl.enabled.mechanisms", mechanism);
    builder.setConfigProp(
        "listener.name.external."
            + mechanism.toLowerCase(java.util.Locale.ROOT)
            + ".sasl.jaas.config",
        jaas);
  }

  private void provisionScramUser(KafkaClusterTestKit candidate) throws Exception {
    // Bootstrap over the unadvertised loopback-only INTERNAL listener. Never use this
    // plaintext admin connection in application tests: they must use clientProperties().
    int internalPort =
        candidate
            .brokers()
            .values()
            .iterator()
            .next()
            .boundPort(ListenerName.normalised(INTERNAL_LISTENER));
    if (internalPort <= 0) {
      throw new IllegalStateException("INTERNAL test listener was not bound");
    }
    Properties bootstrap = new Properties();
    bootstrap.put("bootstrap.servers", "localhost:" + internalPort);
    bootstrap.put("security.protocol", "PLAINTEXT");
    try (Admin admin = Admin.create(bootstrap)) {
      admin
          .alterUserScramCredentials(
              List.of(
                  new UserScramCredentialUpsertion(
                      username,
                      new ScramCredentialInfo(ScramMechanism.SCRAM_SHA_512, 4096),
                      password)))
          .all()
          .get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    }
  }

  public String getKafkaConnectString() {
    return startedCluster().bootstrapServers();
  }

  public SecureKafkaTestUtils getKafkaTestUtils() {
    startedCluster();
    return new SecureKafkaTestUtils(this);
  }

  /** Kafka client configuration for the authenticated client listener only. */
  public Properties clientProperties() {
    return clientProperties(getKafkaConnectString());
  }

  private Properties clientProperties(String bootstrapServers) {
    Properties result = new Properties();
    result.put("bootstrap.servers", bootstrapServers);
    result.put("security.protocol", mode.protocol);
    result.put("request.timeout.ms", "10000");
    result.put("default.api.timeout.ms", "30000");
    if (mode == Mode.SSL) {
      result.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, clientTruststore.toString());
      result.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, clientStorePassword);
      if (clientKeystore != null) {
        result.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, clientKeystore.toString());
        result.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, clientStorePassword);
        if (clientKeyPassword != null) {
          result.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, clientKeyPassword);
        }
      }
      // Intentionally do not override ssl.endpoint.identification.algorithm.
    } else {
      String loginModule =
          mode == Mode.SASL_PLAIN
              ? "org.apache.kafka.common.security.plain.PlainLoginModule"
              : "org.apache.kafka.common.security.scram.ScramLoginModule";
      result.put(SaslConfigs.SASL_MECHANISM, mode == Mode.SASL_PLAIN ? "PLAIN" : "SCRAM-SHA-512");
      result.put(
          SaslConfigs.SASL_JAAS_CONFIG,
          loginModule + " required username=\"" + username + "\" password=\"" + password + "\";");
    }
    return result;
  }

  public void createTopic(String topic) {
    createTopic(topic, 1, (short) 1);
  }

  public void createTopic(String topic, int partitions, short replicationFactor) {
    try (Admin admin = Admin.create(clientProperties())) {
      admin
          .createTopics(List.of(new NewTopic(topic, partitions, replicationFactor)))
          .all()
          .get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while creating test topic: " + topic, e);
    } catch (Exception e) {
      throw new IllegalStateException("Could not create secure Kafka test topic: " + topic, e);
    }
  }

  public <K, V> KafkaProducer<K, V> getKafkaProducer(
      Class<? extends Serializer<K>> keySerializer,
      Class<? extends Serializer<V>> valueSerializer) {
    Properties p = clientProperties();
    p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
    p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
    p.put(ProducerConfig.ACKS_CONFIG, "all");
    return new KafkaProducer<>(p);
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    stop();
  }

  public synchronized void stop() throws Exception {
    KafkaClusterTestKit previous = cluster;
    cluster = null;
    if (previous != null) {
      previous.close();
    }
  }

  private KafkaClusterTestKit startedCluster() {
    KafkaClusterTestKit current = cluster;
    if (current == null) {
      throw new IllegalStateException("Secure Kafka test broker has not been started");
    }
    return current;
  }

  private synchronized void requireNotStarted() {
    if (cluster != null) {
      throw new IllegalStateException("Cannot configure an already started Kafka test broker");
    }
  }

  private void validateTlsStores() {
    if (serverKeystore == null || serverTruststore == null || clientTruststore == null) {
      throw new IllegalStateException(
          "SSL requires server keystore/truststore and client truststore");
    }
    if ("required".equals(clientAuth) && clientKeystore == null) {
      throw new IllegalStateException("mTLS requires a client keystore for startup verification");
    }
  }

  private static String safeCredential(String value) {
    if (value == null || !value.matches("[A-Za-z0-9._-]{1,128}")) {
      throw new IllegalArgumentException(
          "Only test credentials consisting of [A-Za-z0-9._-] are supported");
    }
    return value;
  }

  private static Path existingFile(String path) {
    Path file = Path.of(Objects.requireNonNull(path)).toAbsolutePath().normalize();
    if (!Files.isRegularFile(file)) {
      throw new IllegalArgumentException("Missing TLS test file: " + file);
    }
    return file;
  }
}
