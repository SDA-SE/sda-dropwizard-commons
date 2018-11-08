package com.sdase.commons.server.kafka.testing;

import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import com.sdase.commons.server.testing.Environment;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.stream.Collectors;

public class KafkaBrokerEnvironmentRule implements TestRule {

   private final SharedKafkaTestResource brokeRule;
   static final String CONNECTION_STRING_ENV = "BROKER_CONNECTION_STRING";

   public KafkaBrokerEnvironmentRule(SharedKafkaTestResource brokerRule) {
      this.brokeRule = brokerRule;
   }

   @Override
   public Statement apply(Statement base, Description description) {

      return RuleChain.outerRule(brokeRule).around((base1, description1) -> new Statement() {
         @Override
         public void evaluate() throws Throwable {
            Environment.setEnv(CONNECTION_STRING_ENV , String.format("[ %s ]", brokeRule.getKafkaBrokers().stream().map(b -> "\"" + b.getConnectString() + "\"").collect(Collectors.joining(", "))));
            try {
               base1.evaluate();
            } finally {
               Environment.unsetEnv(CONNECTION_STRING_ENV);
            }
         }
      }).apply(base, description);
   }
}
