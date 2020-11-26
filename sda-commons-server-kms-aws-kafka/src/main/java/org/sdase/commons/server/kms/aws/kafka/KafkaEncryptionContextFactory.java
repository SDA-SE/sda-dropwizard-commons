package org.sdase.commons.server.kms.aws.kafka;

import java.util.Collections;
import java.util.Map;

public class KafkaEncryptionContextFactory {

  private static final String KEY_TOPIC = "topic";

  private KafkaEncryptionContextFactory() {}

  public static Map<String, String> createDefaultEncryptionContext(String topic) {
    return Collections.singletonMap(KEY_TOPIC, topic);
  }
}
