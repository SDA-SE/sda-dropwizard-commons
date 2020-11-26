package org.sdase.commons.server.kms.aws.kafka;

import io.dropwizard.Configuration;
import javax.validation.constraints.NotNull;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.sdase.commons.server.kms.aws.KmsAwsBundle;
import org.sdase.commons.server.kms.aws.KmsAwsDecryptionService;
import org.sdase.commons.server.kms.aws.KmsAwsEncryptionService;
import org.sdase.commons.server.kms.aws.config.KmsAwsConfigurationProvider;

/** Bundle for AWS Client-Side Encryption. Keys are retrieved from AWS KMS */
public class KmsAwsKafkaBundle<C extends Configuration> extends KmsAwsBundle<C> {

  private KmsAwsKafkaBundle(KmsAwsConfigurationProvider<C> configurationProvider) {
    super(configurationProvider);
  }

  public static InitialBuilder builder() {
    return new Builder<>();
  }

  /**
   * Wraps a serializer used for a Kafka message producer to be able to encrypt plaintext messages.
   *
   * @param <T> value object type
   * @param delegate the original Serializer being wrapped
   * @param keyArn the key's ARN to be used for this serializer
   * @return wrapped Serializer capable of encryption using AWS KMS
   */
  public <T> AwsEncryptionSerializer<T> wrapSerializer(Serializer<T> delegate, String keyArn) {
    KmsAwsEncryptionService kmsAwsEncryptionService = createKmsAwsEncryptionService(keyArn);
    return new AwsEncryptionSerializer<>(delegate, kmsAwsEncryptionService);
  }

  /**
   * Wraps a deserializer used for a Kafka message consumer to be able to decrypt encrypted
   * messages.
   *
   * @param delegate the original Deserializer being wrapped
   * @param <T> value object type
   * @return wrapped Deserializer capable of decryption using AWS KMS
   */
  public <T> AwsEncryptionDeserializer<T> wrapDeserializer(Deserializer<T> delegate) {
    KmsAwsDecryptionService kmsAwsDecryptionService = createKmsAwsDecryptionService();
    return new AwsEncryptionDeserializer<>(delegate, kmsAwsDecryptionService);
  }

  public interface InitialBuilder extends KmsAwsBundle.InitialBuilder {
    <C extends Configuration> FinalBuilder<C> withConfigurationProvider(
        @NotNull KmsAwsConfigurationProvider<C> configurationProvider);
  }

  public interface FinalBuilder<C extends Configuration> extends KmsAwsBundle.FinalBuilder<C> {
    KmsAwsKafkaBundle<C> build();
  }

  public static class Builder<T extends Configuration> implements InitialBuilder, FinalBuilder<T> {

    private final KmsAwsConfigurationProvider<T> configProvider;

    private Builder() {
      configProvider = null;
    }

    private Builder(KmsAwsConfigurationProvider<T> configProvider) {
      this.configProvider = configProvider;
    }

    @Override
    public <C extends Configuration> FinalBuilder<C> withConfigurationProvider(
        KmsAwsConfigurationProvider<C> configurationProvider) {
      return new Builder<>(configurationProvider);
    }

    @Override
    public KmsAwsKafkaBundle<T> build() {
      return new KmsAwsKafkaBundle<>(configProvider);
    }
  }
}
