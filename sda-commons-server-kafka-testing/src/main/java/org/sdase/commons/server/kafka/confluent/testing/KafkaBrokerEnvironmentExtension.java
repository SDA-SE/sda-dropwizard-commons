package org.sdase.commons.server.kafka.confluent.testing;

import com.salesforce.kafka.test.KafkaBroker;
import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.sdase.commons.server.testing.Environment;

/** Rule for setting the Environment Variable */
public class KafkaBrokerEnvironmentExtension implements BeforeAllCallback, AfterAllCallback {

  private final SharedKafkaTestResource delegate;
  static final String CONNECTION_STRING_ENV = "BROKER_CONNECTION_STRING";

  public KafkaBrokerEnvironmentExtension(SharedKafkaTestResource brokerRule) {
    this.delegate = brokerRule;
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    delegate.beforeAll(context);
    Environment.setEnv(
        CONNECTION_STRING_ENV,
        String.format(
            "[ %s ]",
            getBrokerConnectStrings().stream()
                .map(b -> "\"" + b + "\"")
                .collect(Collectors.joining(", "))));
  }

  @Override
  public void afterAll(ExtensionContext context) {
    delegate.afterAll(context);
    Environment.unsetEnv(CONNECTION_STRING_ENV);
  }

  public String getConnectString() {
    return delegate.getKafkaConnectString();
  }

  public List<String> getBrokerConnectStrings() {
    return delegate.getKafkaBrokers().stream()
        .map(KafkaBroker::getConnectString)
        .collect(Collectors.toList());
  }

  public SharedKafkaTestResource getSharedKafkaTestResource() {
    return delegate;
  }
}
