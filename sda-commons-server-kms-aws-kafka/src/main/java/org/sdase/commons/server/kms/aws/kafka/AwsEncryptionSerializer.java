package org.sdase.commons.server.kms.aws.kafka;

import java.util.Map;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;
import org.sdase.commons.server.kms.aws.KmsAwsEncryptionService;

public class AwsEncryptionSerializer<T> implements Serializer<T> {

  private final Serializer<T> delegate;
  private final KmsAwsEncryptionService kmsAwsEncryptionService;

  public AwsEncryptionSerializer(
      Serializer<T> delegate, KmsAwsEncryptionService kmsAwsEncryptionService) {
    this.delegate = delegate;
    this.kmsAwsEncryptionService = kmsAwsEncryptionService;
  }

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    delegate.configure(configs, isKey);
  }

  @Override
  public byte[] serialize(String topic, T data) {
    // We always use the default context at the moment. Could be made configurable if we have a
    // suitable use case.
    Map<String, String> context =
        KafkaEncryptionContextFactory.createDefaultEncryptionContext(topic);
    return encrypt(delegate.serialize(topic, data), context);
  }

  @Override
  public byte[] serialize(String topic, Headers headers, T data) {
    return serialize(topic, data);
  }

  @Override
  public void close() {
    delegate.close();
  }

  private byte[] encrypt(byte[] serialized, Map<String, String> encryptionContext) {
    return kmsAwsEncryptionService.encrypt(serialized, encryptionContext);
  }
}
