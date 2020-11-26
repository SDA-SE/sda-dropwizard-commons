package org.sdase.commons.server.kms.aws;

import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;

import com.amazonaws.encryptionsdk.DefaultCryptoMaterialsManager;
import com.amazonaws.encryptionsdk.caching.CachingCryptoMaterialsManager;
import com.amazonaws.encryptionsdk.caching.LocalCryptoMaterialsCache;
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;
import java.util.concurrent.TimeUnit;
import org.mockito.Mockito;
import org.sdase.commons.server.kms.aws.util.MockKMSClient;

public class KmsAwsDecryptionServiceMock {

  private final KmsAwsDecryptionService kmsAwsDecryptionService;

  public KmsAwsDecryptionServiceMock(boolean cachingEnabled, MockKMSClient client) {

    KmsMasterKeyProvider.RegionalClientSupplier supplier =
        mock(KmsMasterKeyProvider.RegionalClientSupplier.class);
    Mockito.when(supplier.getClient(notNull())).thenReturn(client);

    KmsMasterKeyProvider kmsMasterKeyProvider =
        KmsMasterKeyProvider.builder().withCustomClientFactory(supplier).buildDiscovery();

    KmsAwsDecryptionService kmsAwsDecryptionService;

    if (cachingEnabled) {
      CachingCryptoMaterialsManager cryptoMaterialsManager =
          CachingCryptoMaterialsManager.newBuilder()
              .withMasterKeyProvider(kmsMasterKeyProvider)
              .withCache(new LocalCryptoMaterialsCache(10))
              .withMaxAge(10, TimeUnit.SECONDS)
              .withMessageUseLimit(10)
              .build();
      kmsAwsDecryptionService =
          KmsAwsDecryptionService.builder()
              .withCryptoMaterialsManager(cryptoMaterialsManager)
              .build();
    } else {
      kmsAwsDecryptionService =
          KmsAwsDecryptionService.builder()
              .withCryptoMaterialsManager(new DefaultCryptoMaterialsManager(kmsMasterKeyProvider))
              .build();
    }

    this.kmsAwsDecryptionService = kmsAwsDecryptionService;
  }

  public KmsAwsDecryptionService getKmsAwsDecryptionService() {
    return kmsAwsDecryptionService;
  }
}
