package org.sdase.commons.server.kafka.confluent.testing;

import com.salesforce.kafka.test.KafkaBroker;
import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.sdase.commons.server.testing.Environment;

/** Rule for setting the Environment Variable */
public class KafkaBrokerEnvironmentRule implements TestRule, KafkaBrokerRule {

  private final SharedKafkaTestResource kafka;
  static final String CONNECTION_STRING_ENV = "BROKER_CONNECTION_STRING";

  public KafkaBrokerEnvironmentRule(SharedKafkaTestResource brokerRule) {
    this.kafka = brokerRule;
  }

  @Override
  public Statement apply(Statement base, Description description) {

    return RuleChain.outerRule(kafka)
        .around(
            (base1, description1) ->
                new Statement() {
                  @Override
                  public void evaluate() throws Throwable {
                    Environment.setEnv(
                        CONNECTION_STRING_ENV,
                        String.format(
                            "[ %s ]",
                            getBrokerConnectStrings().stream()
                                .map(b -> "\"" + b + "\"")
                                .collect(Collectors.joining(", "))));
                    try {
                      base1.evaluate();
                    } finally {
                      Environment.unsetEnv(CONNECTION_STRING_ENV);
                    }
                  }
                })
        .apply(base, description);
  }

  @Override
  public String getConnectString() {
    return kafka.getKafkaConnectString();
  }

  @Override
  public List<String> getBrokerConnectStrings() {
    return kafka.getKafkaBrokers().stream()
        .map(KafkaBroker::getConnectString)
        .collect(Collectors.toList());
  }
}
