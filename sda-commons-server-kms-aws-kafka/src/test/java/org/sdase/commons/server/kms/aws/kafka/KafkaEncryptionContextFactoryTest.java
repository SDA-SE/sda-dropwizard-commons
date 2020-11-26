package org.sdase.commons.server.kms.aws.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.Test;

public class KafkaEncryptionContextFactoryTest {

  @Test
  public void createDefaultEncryptionContextTest() {
    String input = "foo";

    Map<String, String> map = KafkaEncryptionContextFactory.createDefaultEncryptionContext(input);

    assertThat(map).hasSize(1).containsEntry("topic", input);
  }

  @Test
  public void createDefaultEncryptionContextNullTest() {
    Map<String, String> map = KafkaEncryptionContextFactory.createDefaultEncryptionContext(null);

    assertThat(map).hasSize(1).containsEntry("topic", null);
  }
}
