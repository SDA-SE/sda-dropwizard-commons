package org.sdase.commons.server.kafka.confluent.testing;

import io.confluent.kafka.schemaregistry.client.rest.entities.Schema;
import io.confluent.kafka.schemaregistry.exceptions.SchemaRegistryException;
import io.confluent.kafka.schemaregistry.rest.SchemaRegistryConfig;
import io.confluent.kafka.schemaregistry.rest.SchemaRegistryRestApplication;
import org.eclipse.jetty.server.Server;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;


import java.util.Iterator;
import java.util.Properties;
import java.util.stream.Collectors;


public class ConfluentSchemaRegistryRule implements TestRule {

   private SchemaRegistryRestApplication application;

   private int port;
   private String protocolType;
   private String hostname;
   private KafkaBrokerRule rule;


   private ConfluentSchemaRegistryRule() {
   }

   @Override
   public Statement apply(Statement base, Description description) {
      return RuleChain.outerRule(rule).around((base1, description1) -> new Statement() {
         @Override
         public void evaluate() throws Throwable {
            before();
            try {
               base1.evaluate();
            } finally {
               after();
            }
         }
      }).apply(base, description);
   }


   protected void before() throws Exception {
      Properties schemaRegistryProps = new Properties();

      String bootstrapServerConfig = rule.getBrokerConnectStrings().stream()
            .map(s -> String.format("%s://%s", protocolType, s))
            .collect(Collectors.joining(","));

      schemaRegistryProps.put(SchemaRegistryConfig.LISTENERS_CONFIG, "http://0.0.0.0:" + port);
      schemaRegistryProps.put(SchemaRegistryConfig.HOST_NAME_CONFIG, hostname);
      schemaRegistryProps.put(SchemaRegistryConfig.KAFKASTORE_BOOTSTRAP_SERVERS_CONFIG, bootstrapServerConfig);
      SchemaRegistryConfig config = new SchemaRegistryConfig(schemaRegistryProps);
      application = new SchemaRegistryRestApplication(config);

      Server server = application.createServer();
      server.start();
   }


   protected void after() {
      application.onShutdown();
   }

   public void registerSchema(String subject, int version, int id, String schema) throws SchemaRegistryException {
      Schema restSchema = new Schema(subject, version, id, schema);
      application.schemaRegistry().register(subject, restSchema);
   }

   public Iterator<Schema> getAllSchemaVersions(String subject) throws SchemaRegistryException {
      return application.schemaRegistry().getAllVersions(subject, false);
   }

   public static KafkaBuilder builder() {
      return new Builder();
   }

   public interface FinalBuilder {
      public ConfluentSchemaRegistryRule build();
   }

   public interface RegistryBuilder {
      public FinalBuilder withPort(int port);
      public RegistryBuilder withHostname(String hostname);
   }

   public interface KafkaProtocolBuilder {
      public RegistryBuilder withProtocol(String protocolType);
   }

   public interface KafkaBuilder {

      public KafkaProtocolBuilder withKafkaBrokerRule(KafkaBrokerRule rule);

   }

   private static class Builder implements FinalBuilder, RegistryBuilder, KafkaBuilder, KafkaProtocolBuilder {

      private int port = 8081;
      private String hostname = "localhost";
      private String protocolType;
      private KafkaBrokerRule rule;


      public FinalBuilder withPort(int port) {
         this.port = port;
         return this;
      }

      public RegistryBuilder withHostname(String hostname) {
         this.hostname = hostname;
         return this;
      }

      @Override
      public KafkaProtocolBuilder withKafkaBrokerRule(KafkaBrokerRule rule) {
         this.rule = rule;
         return this;
      }

      @Override
      public RegistryBuilder withProtocol(String protocolType) {
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
