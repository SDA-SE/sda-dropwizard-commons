package org.sdase.commons.server.kafka.confluent.testing;

import org.junit.rules.TestRule;

import java.util.List;

/**
 * Interface to wrap all kind of Broker Rules to be able to get the connection string
 */
public interface KafkaBrokerRule extends TestRule  {

   public String getConnectString();

   public List<String> getBrokerConnectStrings();
}
