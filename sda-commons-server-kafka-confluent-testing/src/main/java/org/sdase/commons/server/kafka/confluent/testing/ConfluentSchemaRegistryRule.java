package org.sdase.commons.server.kafka.confluent.testing;

import io.confluent.kafka.schemaregistry.client.rest.entities.Schema;
import io.confluent.kafka.schemaregistry.exceptions.SchemaRegistryException;
import io.confluent.kafka.schemaregistry.rest.SchemaRegistryConfig;
import io.confluent.kafka.schemaregistry.rest.SchemaRegistryRestApplication;
import io.confluent.rest.Application;
import io.confluent.rest.RestConfig;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.curator.test.InstanceSpec;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Slf4jRequestLog;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rule for starting a Confluent Schema Registry for integration testing issues.
 *
 * <p>At least the Kafka broker must be set within the builder so that the schema registry can
 * connect to the broker.
 *
 * <p>The schema registry is published on localhost with the defined port. If no port is defined, a
 * randomized one is used.
 */
public class ConfluentSchemaRegistryRule implements TestRule {

  private SchemaRegistryRestApplication application;

  private int port;
  private String protocolType;
  private String hostname;
  private KafkaBrokerRule rule;
  private boolean started = false;

  private static final Logger LOG = LoggerFactory.getLogger(ConfluentSchemaRegistryRule.class);

  private ConfluentSchemaRegistryRule() {}

  @Override
  public Statement apply(Statement base, Description description) {
    return RuleChain.outerRule(rule)
        .around(
            (base1, description1) ->
                new Statement() {
                  @Override
                  public void evaluate() throws Throwable {
                    before();
                    try {
                      base1.evaluate();
                    } finally {
                      after();
                    }
                  }
                })
        .apply(base, description);
  }

  protected void before() throws Exception {
    Properties schemaRegistryProps = new Properties();

    String bootstrapServerConfig =
        rule.getBrokerConnectStrings().stream()
            .map(s -> String.format("%s://%s", protocolType, s))
            .collect(Collectors.joining(","));

    schemaRegistryProps.put(RestConfig.LISTENERS_CONFIG, "http://0.0.0.0:" + port);
    schemaRegistryProps.put(SchemaRegistryConfig.HOST_NAME_CONFIG, hostname);
    schemaRegistryProps.put(
        SchemaRegistryConfig.KAFKASTORE_BOOTSTRAP_SERVERS_CONFIG, bootstrapServerConfig);
    SchemaRegistryConfig config = new SchemaRegistryConfig(schemaRegistryProps);
    application = new SchemaRegistryRestApplication(config);
    Server server = application.createServer();
    server.start();
    started = true;
    deactivateLoggingBecauseOfVersionConflicts();
  }

  public int getPort() {
    return port;
  }

  public String getConnectionString() {
    if (!started) {
      throw new IllegalStateException("Cannot access before application is started");
    }
    return String.format(Locale.ROOT, "http://%s:%s", "localhost", port);
  }

  private void deactivateLoggingBecauseOfVersionConflicts() {
    Slf4jRequestLog requestLog = // NOSONAR
        (Slf4jRequestLog) // NOSONAR
            Stream.of(Application.class.getDeclaredFields())
                .filter(f -> "requestLog".equals(f.getName()))
                .findFirst()
                .map(
                    f -> {
                      try {
                        f.setAccessible(true);
                        return f.get(application);
                      } catch (IllegalAccessException e) {
                        LOG.warn(
                            "Error when trying to remove logger for request log. Maybe the application will fail with NoSuchMethodException when logger is still active",
                            e);
                      }
                      return null;
                    })
                .orElse(null);

    Stream.of(Slf4jRequestLog.class.getDeclaredFields()) // NOSONAR
        .filter(f -> f.getName().equals("logger"))
        .findFirst()
        .ifPresent(
            f -> {
              try {
                f.setAccessible(true);
                f.set(requestLog, null);
              } catch (IllegalAccessException e) {
                LOG.warn(
                    "Error when trying to remove logger for request log. Maybe the application will fail with NoSuchMethodException when logger is still active",
                    e);
              }
            });
  }

  protected void after() {
    started = false;
    application.onShutdown();
  }

  public void registerSchema(String subject, int version, int id, String schema)
      throws SchemaRegistryException {
    Schema restSchema = new Schema(subject, version, id, schema);
    application.schemaRegistry().register(subject, restSchema);
  }

  public Iterator<Schema> getAllSchemaVersions(String subject) throws SchemaRegistryException {
    return application.schemaRegistry().getAllVersions(subject, false);
  }

  /** @return A builder for the ConfluentSchemaRegistryRule */
  public static OptionalBuilder builder() {
    return new Builder();
  }

  public interface FinalBuilder {
    /** @return created @{@link ConfluentSchemaRegistryRule} */
    ConfluentSchemaRegistryRule build();
  }

  public interface OptionalBuilder {

    /**
     * set the protocol type used within the connection to the broker.
     *
     * @param protocolType protocol type added to the connection strings<br>
     *     default is 'PLAINTEXT'
     * @return builder
     */
    OptionalBuilder withKafkaProtocol(String protocolType);

    /**
     * @param port port used to connect to the schema registry. If not set, a random port is used.
     * @return builder
     */
    OptionalBuilder withPort(int port);

    /**
     * @param hostname The host name advertised in ZooKeeper. Make sure to set this if running
     *     Schema Registry with multiple nodes.
     * @return builder
     */
    OptionalBuilder withHostname(String hostname);

    /**
     * @param rule Rule that starts the kafka broker to retrieve connections information
     * @return builder
     */
    FinalBuilder withKafkaBrokerRule(KafkaBrokerRule rule);
  }

  private static class Builder implements FinalBuilder, OptionalBuilder {

    private int port = InstanceSpec.getRandomPort();
    private String hostname = "localhost";
    private String protocolType = "PLAINTEXT";
    private KafkaBrokerRule rule;

    public OptionalBuilder withPort(int port) {
      this.port = port;
      return this;
    }

    public OptionalBuilder withHostname(String hostname) {
      this.hostname = hostname;
      return this;
    }

    @Override
    public FinalBuilder withKafkaBrokerRule(KafkaBrokerRule rule) {
      this.rule = rule;
      return this;
    }

    @Override
    public OptionalBuilder withKafkaProtocol(String protocolType) {
      this.protocolType = protocolType;
      return this;
    }

    @Override
    public ConfluentSchemaRegistryRule build() {
      ConfluentSchemaRegistryRule result = new ConfluentSchemaRegistryRule();
      result.rule = rule;
      result.port = port;
      result.hostname = hostname;
      result.protocolType = protocolType;
      return result;
    }
  }
}
