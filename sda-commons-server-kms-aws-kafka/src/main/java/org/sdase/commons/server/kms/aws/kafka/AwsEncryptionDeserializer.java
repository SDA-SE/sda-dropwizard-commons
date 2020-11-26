package org.sdase.commons.server.kms.aws.kafka;

import java.util.Map;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import org.sdase.commons.server.kms.aws.InvalidEncryptionContextException;
import org.sdase.commons.server.kms.aws.KmsAwsDecryptionService;

public class AwsEncryptionDeserializer<T> implements Deserializer<T> {
  private final Deserializer<T> delegate;
  private final KmsAwsDecryptionService kmsAwsDecryptionService;

  public AwsEncryptionDeserializer(
      Deserializer<T> delegate, KmsAwsDecryptionService kmsAwsDecryptionService) {
    this.delegate = delegate;
    this.kmsAwsDecryptionService = kmsAwsDecryptionService;
  }

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    delegate.configure(configs, isKey);
  }

  @Override
  public T deserialize(String topic, byte[] data) {
    Map<String, String> context =
        KafkaEncryptionContextFactory.createDefaultEncryptionContext(topic);
    try {
      return delegate.deserialize(topic, decrypt(data, context));
    } catch (InvalidEncryptionContextException e) {
      throw new SerializationException("EncryptionContext is invalid.");
    }
  }

  @Override
  public T deserialize(String topic, Headers headers, byte[] data) {
    return deserialize(topic, data);
  }

  @Override
  public void close() {
    delegate.close();
  }

  private byte[] decrypt(byte[] data, Map<String, String> context)
      throws InvalidEncryptionContextException {
    return kmsAwsDecryptionService.decrypt(data, context);
  }
}
