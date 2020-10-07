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

  @Test
  public void itShouldConfigureSecurityForAdminConfig() {
    KafkaConfiguration config = new KafkaConfiguration();

    Security sec = new Security();
    sec.setPassword("password");
    sec.setUser("user");
    sec.setProtocol(ProtocolType.SASL_SSL);

    config.setSecurity(sec);

    Properties props = KafkaProperties.forAdminClient(config);

    final String saslJaasConfig =
        "org.apache.kafka.common.security.plain.PlainLoginModule required username='user' password='password';";
    assertThat(props.getProperty("sasl.jaas.config")).isEqualTo(saslJaasConfig);
    assertThat(props.getProperty("sasl.mechanism")).isEqualTo("PLAIN");
  }

  @Test
  public void itShouldUseCustomConfigForAdminConfig() {
    KafkaConfiguration config = new KafkaConfiguration();

    Security sec = new Security();
    sec.setPassword("password");
    sec.setUser("user");
    sec.setProtocol(ProtocolType.SASL_SSL);

    config.setSecurity(sec);

    AdminConfig adminConfig = new AdminConfig();
    adminConfig.getConfig().put("custom.property", "custom.property.value");
    adminConfig.getConfig().put("sasl.jaas.config", "custom.sasl.jaas.config");
    adminConfig.getConfig().put("sasl.mechanism", "SCRAM-SHA-512");
    config.setAdminConfig(adminConfig);

    Properties props = KafkaProperties.forAdminClient(config);

    assertThat(props.getProperty("custom.property")).isEqualTo("custom.property.value");
    assertThat(props.getProperty("sasl.jaas.config")).isEqualTo("custom.sasl.jaas.config");
    assertThat(props.getProperty("sasl.mechanism")).isEqualTo("SCRAM-SHA-512");
  }
}
