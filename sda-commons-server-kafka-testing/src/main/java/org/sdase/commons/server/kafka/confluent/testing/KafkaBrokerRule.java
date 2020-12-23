package org.sdase.commons.server.kafka.confluent.testing;

import java.util.List;
import org.junit.rules.TestRule;

/** Interface to wrap all kind of Broker Rules to be able to get the connection string */
public interface KafkaBrokerRule extends TestRule {

  String getConnectString();

  List<String> getBrokerConnectStrings();
}
