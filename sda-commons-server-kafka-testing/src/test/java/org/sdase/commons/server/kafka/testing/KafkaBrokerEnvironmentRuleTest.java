package org.sdase.commons.server.kafka.testing;

import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import org.assertj.core.api.Assertions;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.stream.Collectors;

public class KafkaBrokerEnvironmentRuleTest {

   private static SharedKafkaTestResource kafkaTestResource = new SharedKafkaTestResource().withBrokers(2);

   @ClassRule
   public static KafkaBrokerEnvironmentRule ruleToTest = new KafkaBrokerEnvironmentRule(kafkaTestResource);


   @Test
   public void shouldBeSetInTest() {

      String compare = "[ " + kafkaTestResource.getKafkaBrokers().stream().map(b -> "\""+b.getConnectString()+"\"").collect(Collectors.joining(", "))+ " ]";
      Assertions.assertThat(compare).isEqualTo(System.getenv(KafkaBrokerEnvironmentRule.CONNECTION_STRING_ENV));
   }



}
