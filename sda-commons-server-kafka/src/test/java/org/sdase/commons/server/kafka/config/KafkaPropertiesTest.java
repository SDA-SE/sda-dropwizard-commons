package org.sdase.commons.server.kafka.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Properties;
import org.apache.kafka.common.config.SaslConfigs;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.kafka.KafkaConfiguration;
import org.sdase.commons.server.kafka.KafkaProperties;

class KafkaPropertiesTest {
  @Test
  void itShouldUseGlobalConfig() {
    KafkaConfiguration config = new KafkaConfiguration();

    config.getConfig().put("setting", "from.global");

    assertThat(KafkaProperties.forProducer(config).get("setting")).isEqualTo("from.global");
    assertThat(KafkaProperties.forConsumer(config).get("setting")).isEqualTo("from.global");
    assertThat(KafkaProperties.forAdminClient(config).get("setting")).isEqualTo("from.global");
  }

  @Test
  void itShouldUseAdminConfigOverGlobalConfig() {
    KafkaConfiguration config = new KafkaConfiguration();

    AdminConfig adminConfig = new AdminConfig();
    adminConfig.getConfig().put("setting", "from.admin");
    config.setAdminConfig(adminConfig);

    config.getConfig().put("setting", "from.global");

    assertThat(KafkaProperties.forProducer(config).get("setting")).isEqualTo("from.global");
    assertThat(KafkaProperties.forConsumer(config).get("setting")).isEqualTo("from.global");
    assertThat(KafkaProperties.forAdminClient(config).get("setting")).isEqualTo("from.admin");
  }

  @Test
  void itShouldBuildSaslStringCorrectly() {
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
  void itShouldConfigureSecurityForAdminConfig() {
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
  void itShouldUseCustomConfigForAdminConfig() {
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

  @Test
  void itShouldBuildSaslStringCorrectlyForSCRAM() {
    KafkaConfiguration config = new KafkaConfiguration();

    Security sec = new Security();
    sec.setPassword("password");
    sec.setUser("user");
    sec.setSaslMechanism("SCRAM-SHA-512");
    sec.setProtocol(ProtocolType.SASL_PLAINTEXT);

    config.setSecurity(sec);

    Properties props = KafkaProperties.forProducer(config);

    final String saslJaasConfig =
        "org.apache.kafka.common.security.scram.ScramLoginModule required username='user' password='password';";
    assertThat(props.getProperty("sasl.jaas.config")).isEqualTo(saslJaasConfig);
  }

  @Test
  void itShouldThrowForUnknownSaslMechanism() {
    KafkaConfiguration config = new KafkaConfiguration();

    Security sec = new Security();
    sec.setPassword("password");
    sec.setUser("user");
    sec.setProtocol(ProtocolType.SASL_SSL);
    sec.setSaslMechanism("OAUTHBEARER");

    config.setSecurity(sec);

    assertThatThrownBy(() -> KafkaProperties.forProducer(config))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unsupported SASL mechanism OAUTHBEARER");
  }

  @Test
  void itShouldNotConfigureSaslForPLAINTEXT() {
    KafkaConfiguration config = new KafkaConfiguration();

    Security sec = new Security();
    sec.setPassword("password");
    sec.setUser("user");
    sec.setSaslMechanism("PLAIN");
    sec.setProtocol(ProtocolType.PLAINTEXT);

    config.setSecurity(sec);

    Properties props = KafkaProperties.forProducer(config);
    assertThat(props.getProperty(SaslConfigs.SASL_JAAS_CONFIG)).isNull();
    assertThat(props.getProperty(SaslConfigs.SASL_MECHANISM)).isNull();
  }

  @Test
  void itShouldNotConfigureSaslForSSL() {
    KafkaConfiguration config = new KafkaConfiguration();

    Security sec = new Security();
    sec.setPassword("password");
    sec.setUser("user");
    sec.setSaslMechanism("PLAIN");
    sec.setProtocol(ProtocolType.SSL);

    config.setSecurity(sec);

    Properties props = KafkaProperties.forProducer(config);
    assertThat(props.getProperty(SaslConfigs.SASL_JAAS_CONFIG)).isNull();
    assertThat(props.getProperty(SaslConfigs.SASL_MECHANISM)).isNull();
  }
}
