package org.sdase.commons.server.kms.aws.testing;

import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CommitmentPolicy;
import com.amazonaws.encryptionsdk.CryptoResult;
import com.amazonaws.encryptionsdk.kms.KmsMasterKey;
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.ClassRule;
import org.junit.Test;

public class KmsAwsRuleIT {
  private static final String KMS_REGION = "test-region";
  private static final String KMS_ACCOUNT = "test-account";
  private static final String KMS_CMK_ALIAS = "alias/testing";

  private final AwsCrypto awsCrypto =
      AwsCrypto.builder()
          .withCommitmentPolicy(CommitmentPolicy.RequireEncryptRequireDecrypt)
          .build();

  @ClassRule public static final KmsAwsRule WIRE_RULE = new KmsAwsRule();

  @Test
  public void shouldEncryptAndDecryptViaWiremock() {
    String plaintext = UUID.randomUUID().toString();
    assertThat(WIRE_RULE.getEndpointUrl()).isNotEmpty().startsWith("http");

    // encrypt
    KmsMasterKeyProvider encryptionProvider = initEncryptionClient();

    Map<String, String> encryptionContext = new HashMap<>();
    encryptionContext.put("foo", "bar");
    CryptoResult<byte[], KmsMasterKey> cryptoResult =
        awsCrypto.encryptData(
            encryptionProvider, plaintext.getBytes(StandardCharsets.UTF_8), encryptionContext);

    assertThat(cryptoResult.getResult()).isNotEmpty();
    assertThat(cryptoResult.getMasterKeyIds()).isNotEmpty();
    assertThat(cryptoResult.getMasterKeyIds().get(0)).isNotEmpty();

    // decrypt
    KmsMasterKeyProvider decryptionProvider = initDecryptionClient();
    CryptoResult<byte[], KmsMasterKey> decryptData =
        awsCrypto.decryptData(decryptionProvider, cryptoResult.getResult());

    assertThat(decryptData.getEncryptionContext()).isNotEmpty();
    assertThat(decryptData.getEncryptionContext()).containsEntry("foo", "bar");
    assertThat(decryptData.getResult()).isNotEmpty();
    assertThat(new String(decryptData.getResult(), StandardCharsets.UTF_8)).isEqualTo(plaintext);
  }

  private KmsMasterKeyProvider initEncryptionClient() {
    String keyArn = buildArnFromAlias(KMS_REGION, KMS_ACCOUNT, KMS_CMK_ALIAS);

    return initPlain().withDefaultRegion(KMS_REGION).buildStrict(keyArn);
  }

  private KmsMasterKeyProvider initDecryptionClient() {
    return initPlain().buildDiscovery();
  }

  private KmsMasterKeyProvider.Builder initPlain() {
    return KmsMasterKeyProvider.builder()
        .withCustomClientFactory(
            regionName ->
                AWSKMSClientBuilder.standard()
                    .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                            WIRE_RULE.getEndpointUrl(), regionName))
                    .withCredentials(
                        new AWSStaticCredentialsProvider(new BasicAWSCredentials("foo", "bar")))
                    .build());
  }

  private String buildArnFromAlias(String region, String accountId, String keyAlias) {
    return "arn:aws:kms:" + region + ":" + accountId + ":" + keyAlias;
  }
}
