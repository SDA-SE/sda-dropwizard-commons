package org.sdase.commons.server.kafka.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.Test;
import org.sdase.commons.server.kafka.KafkaConfiguration;
import org.sdase.commons.server.kafka.KafkaProperties;

public class KafkaPropertiesSpec {

  @Test
  public void itShouldBuildSaslStringCorrectly() {
    KafkaConfiguration config = new KafkaConfiguration();

    Security sec = new Security();
    sec.setPassword("password");
    sec.setUser("user");
    sec.setProtocol(ProtocolType.SASL_SSL);

    config.setSecurity(sec);

    Properties props = KafkaProperties.forProducer(config);

    final String saslJaasConfig =
        "org.apache.kafka.common.security.plain.PlainLoginModule required username='user' password='password';";
    assertThat(props.getProperty("sasl.jaas.config")).isEqualTo(saslJaasConfig);
  }
}
