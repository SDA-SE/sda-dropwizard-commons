package org.sdase.commons.server.auth.key;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RsaKeyLoaderSchedulerTest {
  private static final int RELOAD_PERIOD = 1;

  // test data
  private final List<LoadedPublicKey> actualKeys = new ArrayList<>();
  private final AtomicBoolean actualInvocationFails = new AtomicBoolean();

  // track invocations
  private final AtomicInteger numberOfReloads = new AtomicInteger(0);

  // setup
  private final RsaPublicKeyLoader keyLoader = new RsaPublicKeyLoader();
  private final ScheduledExecutorService executorService =
      Executors.newSingleThreadScheduledExecutor();
  private final RsaKeyLoaderScheduler keyLoaderScheduler =
      RsaKeyLoaderScheduler.create(keyLoader, executorService);
  private final KeySource keySource =
      () -> {
        numberOfReloads.incrementAndGet();
        if (actualInvocationFails.get()) {
          throw new RuntimeException("Some error occurred during reload");
        }
        return actualKeys;
      };

  @Before
  public void startKeyLoader() {
    keyLoader.addKeySource(keySource);
    keyLoaderScheduler.internalStart(0, RELOAD_PERIOD, TimeUnit.SECONDS);
    // wait for initial load
    waitForNextReload();
  }

  @After
  public void shutdownExecutorService() {
    executorService.shutdown();
  }

  @Test
  public void shouldRemoveRevokedKey() {
    // given
    RSAPublicKey validKey = mock(RSAPublicKey.class);
    RSAPublicKey keyToBeRevoked = mock(RSAPublicKey.class);

    // initially, two known keys are published
    actualKeys.add(new LoadedPublicKey("kidForValidKey", validKey, keySource, null));
    actualKeys.add(new LoadedPublicKey("kidForKeyToBeRevoked", keyToBeRevoked, keySource, null));

    waitForNextReload();
    assertThatKeyIsKnown("kidForValidKey", validKey);
    assertThatKeyIsKnown("kidForKeyToBeRevoked", keyToBeRevoked);

    // simulate revoking of key:
    actualKeys.removeIf(loadedPublicKey -> loadedPublicKey.getKid().equals("kidForKeyToBeRevoked"));

    waitForNextReload();
    assertThatKeyIsKnown("kidForValidKey", validKey);
    assertThatKeyIsUnknown("kidForKeyToBeRevoked");
  }

  @Test
  public void shouldKeepKeysOnLoadingFailure() {
    final RSAPublicKey validKey = mock(RSAPublicKey.class);

    actualKeys.add(new LoadedPublicKey("kidForValidKey", validKey, keySource, null));

    waitForNextReload();
    assertThatKeyIsKnown("kidForValidKey", validKey);

    // simulate keyLoad-failure:
    actualInvocationFails.set(true);
    waitForNextReload();
    // key is found anyway:
    assertThatKeyIsKnown("kidForValidKey", validKey);
  }

  @Test
  public void shouldContinueScheduleAfterLoadFailure() {
    final RSAPublicKey validKey = mock(RSAPublicKey.class);

    // start empty
    waitForNextReload();

    // introduce error
    actualInvocationFails.set(true);
    waitForNextReload();

    // expect more schedules
    actualInvocationFails.set(false);
    actualKeys.add(new LoadedPublicKey("kidForValidKey", validKey, keySource, null));

    waitForNextReload();
    assertThatKeyIsKnown("kidForValidKey", validKey);
  }

  private void assertThatKeyIsUnknown(String kid) {
    assertThat(keyLoader.getLoadedPublicKey(kid)).isNull();
  }

  private void assertThatKeyIsKnown(String kid, RSAPublicKey expectedKeyInstance) {
    assertThat(keyLoader.getLoadedPublicKey(kid).getPublicKey()).isSameAs(expectedKeyInstance);
  }

  private void waitForNextReload() {
    int expectedReloads = numberOfReloads.get() + 1;
    await()
        .untilAsserted(
            () -> assertThat(numberOfReloads).hasValueGreaterThanOrEqualTo(expectedReloads));
  }
}
