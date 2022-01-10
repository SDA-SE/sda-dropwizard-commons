package org.sdase.commons.server.kafka;

import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.security.plain.PlainLoginModule;
import org.apache.kafka.common.security.scram.ScramLoginModule;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.sdase.commons.server.kafka.config.ProtocolType;
import org.sdase.commons.server.kafka.config.Security;

public class KafkaProperties extends Properties {

  /** */
  private static final long serialVersionUID = -4196815076149945888L;

  private KafkaProperties() {
    //
  }

  private static Properties configureSecurity(Security security) {
    KafkaProperties props = new KafkaProperties();

    if (security == null) {
      return props;
    }

    if (security.getProtocol() != null) {
      props.put("security.protocol", security.getProtocol().name());
    }

    if (ProtocolType.isSasl(security.getProtocol())
        && security.getPassword() != null
        && security.getUser() != null) {
      props.put("sasl.mechanism", security.getSaslMechanism());
      props.put(
          "sasl.jaas.config",
          String.format(
              "%s required username='%s' password='%s';",
              getLoginModule(security.getSaslMechanism()).getName(),
              security.getUser(),
              security.getPassword()));
    }

    return props;
  }

  private static KafkaProperties baseProperties(KafkaConfiguration configuration) {

    KafkaProperties props = new KafkaProperties();

    if (configuration.getBrokers() != null) {
      props.put(
          CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
          String.join(",", configuration.getBrokers()));
    }
    props.putAll(configureSecurity(configuration.getSecurity()));

    if (configuration.getConfig() != null) {
      props.putAll(configuration.getConfig());
    }

    return props;
  }

  private static KafkaProperties adminProperties(KafkaConfiguration configuration) {

    KafkaProperties props = new KafkaProperties();

    // If AdminEndpoint is not set, the base configuration is used because no add
    if (configuration.getAdminConfig() == null
        || configuration.getAdminConfig().getAdminEndpoint() == null
        || configuration.getAdminConfig().getAdminEndpoint().isEmpty()
        || StringUtils.isBlank(
            String.join(",", configuration.getAdminConfig().getAdminEndpoint()))) {
      props = baseProperties(configuration);
      return props;
    }

    props.put(
        CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
        String.join(",", configuration.getAdminConfig().getAdminEndpoint()));

    props.putAll(configureSecurity(configuration.getAdminConfig().getAdminSecurity()));

    return props;
  }

  public static KafkaProperties forAdminClient(KafkaConfiguration configuration) {
    KafkaProperties props = adminProperties(configuration);
    props.put(
        AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG,
        configuration.getAdminConfig().getAdminClientRequestTimeoutMs());
    // there was a change in kafka (issue KIP-533) that introduced separate timeout for api calls
    props.put(
        AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG,
        configuration.getAdminConfig().getAdminClientRequestTimeoutMs());
    props.putAll(configuration.getAdminConfig().getConfig());
    return props;
  }

  public static KafkaProperties forConsumer(KafkaConfiguration configuration) {
    KafkaProperties props = baseProperties(configuration);

    props.put(ConsumerConfig.GROUP_ID_CONFIG, "default");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
    props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, String.valueOf(1000));
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

    return props;
  }

  public static KafkaProperties forProducer(KafkaConfiguration configuration) {
    KafkaProperties props = baseProperties(configuration);

    props.put(ProducerConfig.ACKS_CONFIG, "all");
    props.put(ProducerConfig.RETRIES_CONFIG, "0");
    props.put(ProducerConfig.LINGER_MS_CONFIG, "0");
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

    return props;
  }

  private static Class<?> getLoginModule(String saslMechanism) {
    switch (saslMechanism.toUpperCase()) {
      case "PLAIN":
        return PlainLoginModule.class;
      case "SCRAM-SHA-256":
      case "SCRAM-SHA-512":
        return ScramLoginModule.class;
      default:
        throw new IllegalArgumentException("Unsupported SASL mechanism " + saslMechanism);
    }
  }
}
