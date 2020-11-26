package org.sdase.commons.server.kms.aws;

import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.amazonaws.encryptionsdk.DefaultCryptoMaterialsManager;
import com.amazonaws.encryptionsdk.caching.CachingCryptoMaterialsManager;
import com.amazonaws.encryptionsdk.caching.LocalCryptoMaterialsCache;
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;
import java.util.concurrent.TimeUnit;
import org.mockito.Mockito;
import org.sdase.commons.server.kms.aws.util.MockKMSClient;

public class KmsAwsEncryptionServiceMock {

  private final KmsAwsEncryptionService kmsAwsEncryptionService;
  private final MockKMSClient client;

  public KmsAwsEncryptionServiceMock(boolean cachingEnabled) {

    client = spy(new MockKMSClient());

    KmsMasterKeyProvider.RegionalClientSupplier supplier =
        mock(KmsMasterKeyProvider.RegionalClientSupplier.class);
    Mockito.when(supplier.getClient(notNull())).thenReturn(client);

    String keyArn = client.createKey().getKeyMetadata().getArn();

    KmsMasterKeyProvider mkp0 =
        KmsMasterKeyProvider.builder().withCustomClientFactory(supplier).buildStrict(keyArn);

    KmsAwsEncryptionService kmsAwsEncryptionService;
    if (cachingEnabled) {
      CachingCryptoMaterialsManager cryptoMaterialsManager =
          CachingCryptoMaterialsManager.newBuilder()
              .withMasterKeyProvider(mkp0)
              .withCache(new LocalCryptoMaterialsCache(10))
              .withMaxAge(10, TimeUnit.SECONDS)
              .withMessageUseLimit(10)
              .build();
      kmsAwsEncryptionService =
          KmsAwsEncryptionService.builder()
              .withCryptoMaterialsManager(cryptoMaterialsManager)
              .build();
    } else {
      kmsAwsEncryptionService =
          KmsAwsEncryptionService.builder()
              .withCryptoMaterialsManager(new DefaultCryptoMaterialsManager(mkp0))
              .build();
    }

    this.kmsAwsEncryptionService = kmsAwsEncryptionService;
  }

  public KmsAwsEncryptionService getKmsAwsEncryptionService() {
    return kmsAwsEncryptionService;
  }

  public MockKMSClient getMockKmsClient() {
    return this.client;
  }
}
