package com.salesforce.kafka.test;

import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import com.salesforce.kafka.test.junit5.SharedZookeeperTestResource;
import com.salesforce.kafka.test.listeners.BrokerListener;
import com.salesforce.kafka.test.listeners.SaslScramListener;
import java.lang.reflect.Field;
import kafka.admin.ConfigCommand;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Creates and starts up a {@link SharedKafkaTestResource} that uses a custom {@link
 * SharedZookeeperTestResource} with installed SCRAM-SHA-* credentials.
 *
 * <p>See {@link SharedKafkaTestResource} for usage.
 */
public class SharedKafkaClassExtensionScram extends SharedKafkaTestResource {
  private final SharedZookeeperTestResource sharedZookeeperTestResource =
      new SharedZookeeperTestResource();

  @Override
  protected void setKafkaCluster(KafkaCluster kafkaCluster) {
    // we need to replace the zkTestServer in the kafka cluster in order to change the Zookeper
    // configuration after it started and before the kafka broker starts.
    if (kafkaCluster instanceof KafkaTestCluster) {
      try {
        Field f = KafkaTestCluster.class.getDeclaredField("zkTestServer");

        // the field is private, so we need to make it accessible
        f.setAccessible(true);

        // use the already started (and configured) zookeeper server.
        f.set(kafkaCluster, sharedZookeeperTestResource.getZookeeperTestServer());
      } catch (Exception e) {
        throw new RuntimeException("Error on replacing the zkTestServer", e);
      }
    }

    super.setKafkaCluster(kafkaCluster);
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    // First start Zookeeper to be able to change the configuration
    sharedZookeeperTestResource.beforeAll(context);
    configureScramUser("admin", "admin-secret");
    configureRegisteredUser();
    super.beforeAll(context);
  }

  @Override
  public void afterAll(ExtensionContext context) {
    super.afterAll(context);
    sharedZookeeperTestResource.afterAll(context);
  }

  private void configureRegisteredUser() {
    // create the user that was registered in the SaslScramListener
    BrokerListener registeredListener = getRegisteredListener();
    if (registeredListener instanceof SaslScramListener) {
      configureScramUser(
          ((SaslScramListener) registeredListener).getUsername(),
          ((SaslScramListener) registeredListener).getPassword());
    }
  }

  /**
   * Calls a command that sets up SCRAM-SHA-* credentials in the zookeeper. There is no other
   * possibility to do this yet (see also
   * https://cwiki.apache.org/confluence/display/KAFKA/KIP-506%3A+Allow+setting+SCRAM+password+via+Admin+interface).
   *
   * @param username the name of the user to update
   * @param password the password to set
   */
  private void configureScramUser(String username, String password) {
    ConfigCommand.main(
        new String[] {
          "--zookeeper",
          sharedZookeeperTestResource.getZookeeperConnectString(),
          "--alter",
          "--add-config",
          "SCRAM-SHA-256=[password=" + password + "],SCRAM-SHA-512=[password=" + password + "]",
          "--entity-type",
          "users",
          "--entity-name",
          username
        });
  }
}
