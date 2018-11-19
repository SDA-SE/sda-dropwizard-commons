package org.sdase.commons.server.kafka.confluent.testing;

import org.junit.rules.TestRule;

import java.util.List;

public interface KafkaBrokerRule extends TestRule  {

   public String getConnectString();

   public List<String> getBrokerConnectStrings();
}
