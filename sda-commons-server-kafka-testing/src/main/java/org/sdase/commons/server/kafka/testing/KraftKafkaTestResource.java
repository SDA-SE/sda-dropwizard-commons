package org.sdase.commons.server.kafka.testing;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.test.KafkaClusterTestKit;
import org.apache.kafka.common.test.TestKitNodes;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Docker-free embedded Kafka KRaft cluster for JUnit 5 tests.
 *
 * <p>Register before the Dropwizard application extension.
 */
public class KraftKafkaTestResource implements BeforeAllCallback, AfterAllCallback {
  private KafkaClusterTestKit cluster;
  private int brokerCount = 1;
  private final Map<String, Object> brokerProperties = new HashMap<>();

  public KraftKafkaTestResource withBrokerProperty(String name, String value) {
    brokerProperties.put(name, value);
    return this;
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    TestKitNodes nodes =
        new TestKitNodes.Builder()
            .setCombined(brokerCount == 1)
            .setNumBrokerNodes(brokerCount)
            .setNumControllerNodes(1)
            .build();

    KafkaClusterTestKit.Builder builder = new KafkaClusterTestKit.Builder(nodes);

    builder.setDeleteOnClose(false);
    builder.setConfigProp("offsets.topic.replication.factor", "1");
    builder.setConfigProp("transaction.state.log.replication.factor", "1");
    builder.setConfigProp("transaction.state.log.min.isr", "1");
    builder.setConfigProp("group.initial.rebalance.delay.ms", "0");

    brokerProperties.forEach(builder::setConfigProp);

    cluster = builder.build();

    try {
      cluster.format();
      cluster.startup();
      cluster.waitForReadyBrokers();
    } catch (Exception e) {
      try {
        cluster.close();
      } catch (Exception closeError) {
        e.addSuppressed(closeError);
      }
      cluster = null;
      throw e;
    }
  }

  @Override
  public void afterAll(ExtensionContext context) {
    stop();
  }

  /** Stops the embedded Kafka cluster, e.g. to simulate a broker outage in a test. */
  public void stop() {
    if (cluster != null) {
      try {
        cluster.close();
      } catch (Exception e) {
        throw new IllegalStateException("Failed to stop embedded Kafka cluster", e);
      } finally {
        cluster = null;
      }
    }
  }

  public String getKafkaConnectString() {
    if (cluster == null) {
      throw new IllegalStateException("Kafka cluster has not been started");
    }
    return cluster.bootstrapServers();
  }

  public KraftKafkaTestUtils getKafkaTestUtils() {
    if (cluster == null) {
      throw new IllegalStateException("Kafka cluster has not been started");
    }
    return new KraftKafkaTestUtils(cluster.bootstrapServers());
  }
}
