package org.sdase.commons.server.auth.key;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Test;

public class RsaKeyLoaderSchedulerTest {
  private static final int RELOAD_PERIOD = 2;
  private final RsaPublicKeyLoader keyLoader = new RsaPublicKeyLoader();
  private final ScheduledExecutorService executorService =
      Executors.newSingleThreadScheduledExecutor();
  private final RsaKeyLoaderScheduler keyLoaderScheduler =
      RsaKeyLoaderScheduler.create(keyLoader, executorService);

  @Test
  public void shouldNotBeAuthenticatedWithRevokedKey() {
    // given
    final AtomicInteger numberOfCalls = new AtomicInteger(0);
    final AtomicBoolean keyIsRevoked = new AtomicBoolean(false);
    final RSAPublicKey validKey = mock(RSAPublicKey.class);
    final String kidForValidKey = "kidForValidKey";
    final RSAPublicKey keyToBeRevoked = mock(RSAPublicKey.class);
    final String kidForKeyToBeRevoked = "kidForKeyToBeRevoked";

    KeySource keySource =
        new KeySource() {
          @Override
          public List<LoadedPublicKey> loadKeysFromSource() throws KeyLoadFailedException {
            numberOfCalls.incrementAndGet();
            if (!keyIsRevoked.get()) {
              return Arrays.asList(
                  new LoadedPublicKey(kidForValidKey, validKey, this),
                  new LoadedPublicKey(kidForKeyToBeRevoked, keyToBeRevoked, this));
            } else {
              return singletonList(new LoadedPublicKey("kidForValidKey", validKey, this));
            }
          }
        };

    keyLoader.addKeySource(keySource);
    keyLoaderScheduler.internalStart(0, RELOAD_PERIOD, TimeUnit.SECONDS);

    await().untilAsserted(() -> assertThat(numberOfCalls.get()).isEqualTo(2));
    assertThat(keyLoader.getKey(kidForValidKey)).isSameAs(validKey);
    assertThat(keyLoader.getKey(kidForKeyToBeRevoked)).isSameAs(keyToBeRevoked);

    // simulate revoking of key:
    keyIsRevoked.set(true);
    await().untilAsserted(() -> assertThat(numberOfCalls.get()).isEqualTo(3));
    assertThat(keyLoader.getKey(kidForValidKey)).isSameAs(validKey);
    // revoked key not found anymore
    assertThat(keyLoader.getKey(kidForKeyToBeRevoked)).isNull();
  }

  @Test
  public void shouldContinueJobProperlyAfterKeyLoadingFailure() {
    final AtomicInteger numberOfCalls = new AtomicInteger(0);
    final AtomicBoolean keyLoadShouldFail = new AtomicBoolean(false);
    final AtomicBoolean anErrorHasOccurred = new AtomicBoolean(false);
    final RSAPublicKey validKey = mock(RSAPublicKey.class);
    final String kidForValidKey = "kidForValidKey";

    KeySource keySource =
        new KeySource() {
          @Override
          public List<LoadedPublicKey> loadKeysFromSource() throws KeyLoadFailedException {
            numberOfCalls.incrementAndGet();
            if (keyLoadShouldFail.get()) {
              anErrorHasOccurred.set(true);
              throw new KeyLoadFailedException("Some error occurred during reload");
            } else {
              return singletonList(new LoadedPublicKey(kidForValidKey, validKey, this));
            }
          }
        };

    keyLoader.addKeySource(keySource);
    keyLoaderScheduler.internalStart(0, RELOAD_PERIOD, TimeUnit.SECONDS);

    await().untilAsserted(() -> assertThat(numberOfCalls.get()).isEqualTo(2));
    assertThat(keyLoader.getKey(kidForValidKey)).isSameAs(validKey);

    // simulate keyLoad-failure:
    keyLoadShouldFail.set(true);
    await().untilAsserted(() -> assertThat(numberOfCalls.get()).isEqualTo(3));
    assertThat(anErrorHasOccurred.get()).isEqualTo(true);
    // key is found anyway:
    assertThat(keyLoader.getKey(kidForValidKey)).isSameAs(validKey);
  }

  @After
  public void shutDown() {
    executorService.shutdown();
  }
}
