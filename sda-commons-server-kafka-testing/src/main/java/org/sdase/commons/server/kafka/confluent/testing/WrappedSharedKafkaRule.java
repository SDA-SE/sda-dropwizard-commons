package org.sdase.commons.server.kafka.confluent.testing;

import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import org.junit.rules.RuleChain;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.List;
import java.util.stream.Collectors;

public class WrappedSharedKafkaRule implements KafkaBrokerRule {

   private final SharedKafkaTestResource brokeRule;

   public WrappedSharedKafkaRule(SharedKafkaTestResource rule) {
      this.brokeRule = rule;
   }

   @Override
   public Statement apply(Statement base, Description description) {
      return RuleChain.outerRule(brokeRule).apply(base, description);
   }

   @Override
   public String getConnectString() {
      return brokeRule.getKafkaConnectString();
   }

   @Override
   public List<String> getBrokerConnectStrings() {
      return brokeRule.getKafkaBrokers().stream().map(b -> b.getConnectString()).collect(Collectors.toList());
   }
}
