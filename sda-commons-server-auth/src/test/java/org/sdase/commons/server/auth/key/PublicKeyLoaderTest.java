package org.sdase.commons.server.auth.key;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PublicKeyLoaderTest {

  private PublicKeyLoader keyLoader = new PublicKeyLoader();

  @Test
  void shouldAddKeyWithoutKid() {
    RSAPublicKey mockKey = Mockito.mock(RSAPublicKey.class);
    KeySource keySource =
        new KeySource() {
          @Override
          public List<LoadedPublicKey> loadKeysFromSource() throws KeyLoadFailedException {
            return singletonList(new LoadedPublicKey(null, null, mockKey, this, null, null));
          }
        };

    keyLoader.addKeySource(keySource);

    assertThat(keyLoader.getKeysWithoutAnyId().stream().map(LoadedPublicKey::getPublicKey))
        .containsExactly(mockKey);
    assertThat(keyLoader.getLoadedPublicKey(null, null)).isNull();
  }

  @Test
  void shouldAddKeyWithKid() {
    RSAPublicKey mockKey = Mockito.mock(RSAPublicKey.class);
    KeySource keySource =
        new KeySource() {
          @Override
          public List<LoadedPublicKey> loadKeysFromSource() throws KeyLoadFailedException {
            return singletonList(
                new LoadedPublicKey("exampleKid", null, mockKey, this, null, "RS256"));
          }
        };

    keyLoader.addKeySource(keySource);

    assertThat(keyLoader.getKeysWithoutAnyId()).isEmpty();
    assertThat(keyLoader.getLoadedPublicKey("exampleKid", null).getPublicKey()).isSameAs(mockKey);
  }

  @Test
  void shouldAddKeyWithX5t() {
    RSAPublicKey mockKey = Mockito.mock(RSAPublicKey.class);
    KeySource keySource =
        new KeySource() {
          @Override
          public List<LoadedPublicKey> loadKeysFromSource() throws KeyLoadFailedException {
            return singletonList(
                new LoadedPublicKey(null, "x5tExample", mockKey, this, null, "RS256"));
          }
        };

    keyLoader.addKeySource(keySource);

    assertThat(keyLoader.getKeysWithoutAnyId()).isEmpty();
    assertThat(keyLoader.getLoadedPublicKey(null, "x5tExample").getPublicKey()).isSameAs(mockKey);
  }

  @Test
  void shouldAddKeyWithX5tAndKid() {
    RSAPublicKey mockKey = Mockito.mock(RSAPublicKey.class);
    KeySource keySource =
        new KeySource() {
          @Override
          public List<LoadedPublicKey> loadKeysFromSource() throws KeyLoadFailedException {
            return singletonList(
                new LoadedPublicKey("kidExample", "x5tExample", mockKey, this, null, "RS256"));
          }
        };

    keyLoader.addKeySource(keySource);

    assertThat(keyLoader.getKeysWithoutAnyId()).isEmpty();
    assertThat(keyLoader.getLoadedPublicKey(null, "x5tExample").getPublicKey()).isSameAs(mockKey);
    assertThat(keyLoader.getLoadedPublicKey("kidExample", null).getPublicKey()).isSameAs(mockKey);
    assertThat(keyLoader.getLoadedPublicKey("kidExample", "x5tExample").getPublicKey())
        .isSameAs(mockKey);
  }

  @Test
  void shouldKidWithHigherPrecedenceBeforeX5t() {
    RSAPublicKey mockKey = Mockito.mock(RSAPublicKey.class);
    RSAPublicKey newMockKey = Mockito.mock(RSAPublicKey.class);

    KeySource keySource =
        new KeySource() {
          @Override
          public List<LoadedPublicKey> loadKeysFromSource() throws KeyLoadFailedException {
            return Arrays.asList(
                new LoadedPublicKey("exampleKid", "exampleX5t", mockKey, this, null, "RS256"),
                new LoadedPublicKey("newKid", "exampleX5t", newMockKey, this, null, "RS256"));
          }
        };

    keyLoader.addKeySource(keySource);

    assertThat(keyLoader.getKeysWithoutAnyId()).isEmpty();
    assertThat(keyLoader.getLoadedPublicKey(null, "exampleX5t").getPublicKey())
        .isSameAs(newMockKey);
    assertThat(keyLoader.getLoadedPublicKey("exampleKid", null).getPublicKey()).isSameAs(mockKey);
    assertThat(keyLoader.getLoadedPublicKey("exampleKid", "exampleX5t").getPublicKey())
        .isSameAs(mockKey);
    assertThat(keyLoader.getLoadedPublicKey("newKid", "exampleX5t").getPublicKey())
        .isSameAs(newMockKey);
  }

  @Test
  void shouldReloadIfKeyIsNotFoundAndRemoveOutdatedKeys() {
    RSAPublicKey mockKey = Mockito.mock(RSAPublicKey.class);
    RSAPublicKey newMockKey = Mockito.mock(RSAPublicKey.class);
    AtomicInteger numberOfCalls = new AtomicInteger();
    KeySource keySource =
        new KeySource() {
          @Override
          public List<LoadedPublicKey> loadKeysFromSource() throws KeyLoadFailedException {
            if (numberOfCalls.getAndIncrement() < 1) {
              return singletonList(
                  new LoadedPublicKey("exampleKid", "exampleX5t", mockKey, this, null, "RS256"));
            } else {
              return singletonList(
                  new LoadedPublicKey("newKid", "newX5t", newMockKey, this, null, "RS256"));
            }
          }
        };

    keyLoader.addKeySource(keySource);

    assertThat(keyLoader.getLoadedPublicKey("exampleKid", null).getPublicKey()).isSameAs(mockKey);
    assertThat(numberOfCalls.get()).isEqualTo(1);

    assertThat(keyLoader.getLoadedPublicKey("exampleKid", null).getPublicKey()).isSameAs(mockKey);
    // no reload for known key
    assertThat(numberOfCalls.get()).isEqualTo(1);

    assertThat(keyLoader.getLoadedPublicKey(null, "exampleX5t").getPublicKey()).isSameAs(mockKey);
    // no reload for known key
    assertThat(numberOfCalls.get()).isEqualTo(1);

    assertThat(keyLoader.getLoadedPublicKey("newKid", null).getPublicKey()).isSameAs(newMockKey);
    assertThat(numberOfCalls.get()).isEqualTo(2);

    assertThat(keyLoader.getLoadedPublicKey(null, "newX5t").getPublicKey()).isSameAs(newMockKey);
    // no reload for known key
    assertThat(numberOfCalls.get()).isEqualTo(2);

    assertThat(keyLoader.getLoadedPublicKey("exampleKid", "exampleX5t")).isNull();
    assertThat(numberOfCalls.get()).isEqualTo(3);
  }

  @Test
  void shouldReloadIfKeyIsNotFound() {
    RSAPublicKey mockKey = Mockito.mock(RSAPublicKey.class);
    RSAPublicKey newMockKey = Mockito.mock(RSAPublicKey.class);
    AtomicInteger numberOfCalls = new AtomicInteger();
    KeySource keySource =
        new KeySource() {
          @Override
          public List<LoadedPublicKey> loadKeysFromSource() throws KeyLoadFailedException {
            if (numberOfCalls.getAndIncrement() < 1) {
              return singletonList(
                  new LoadedPublicKey("exampleKid", "exampleX5t", mockKey, this, null, "RS256"));
            } else {
              return Arrays.asList(
                  new LoadedPublicKey("exampleKid", "exampleX5t", mockKey, this, null, "RS256"),
                  new LoadedPublicKey("newKid", "newX5t", newMockKey, this, null, "RS256"));
            }
          }
        };

    keyLoader.addKeySource(keySource);

    assertThat(keyLoader.getLoadedPublicKey("exampleKid", null).getPublicKey()).isSameAs(mockKey);
    assertThat(numberOfCalls.get()).isEqualTo(1);

    assertThat(keyLoader.getLoadedPublicKey("exampleKid", null).getPublicKey()).isSameAs(mockKey);
    // no reload for known key
    assertThat(numberOfCalls.get()).isEqualTo(1);

    assertThat(keyLoader.getLoadedPublicKey("newKid", null).getPublicKey()).isSameAs(newMockKey);
    assertThat(numberOfCalls.get()).isEqualTo(2);

    assertThat(keyLoader.getLoadedPublicKey("exampleKid", null).getPublicKey()).isSameAs(mockKey);
    assertThat(numberOfCalls.get()).isEqualTo(2);
  }

  @Test
  void shouldNotRemoveIfReloadFails() {
    RSAPublicKey mockKey = Mockito.mock(RSAPublicKey.class);
    AtomicInteger numberOfCalls = new AtomicInteger();
    KeySource keySource =
        new KeySource() {
          @Override
          public List<LoadedPublicKey> loadKeysFromSource() throws KeyLoadFailedException {
            if (numberOfCalls.getAndIncrement() < 1) {
              return singletonList(
                  new LoadedPublicKey("exampleKid", "exampleX5t", mockKey, this, null, "RS256"));
            } else {
              throw new KeyLoadFailedException("Test error");
            }
          }
        };

    keyLoader.addKeySource(keySource);

    assertThat(keyLoader.getLoadedPublicKey("exampleKid", null).getPublicKey()).isSameAs(mockKey);
    assertThat(numberOfCalls.get()).isEqualTo(1);

    assertThat(keyLoader.getLoadedPublicKey("exampleKid", null).getPublicKey()).isSameAs(mockKey);
    // no reload for known key
    assertThat(numberOfCalls.get()).isEqualTo(1);

    assertThat(keyLoader.getLoadedPublicKey("newKid", null)).isNull();
    assertThat(numberOfCalls.get()).isEqualTo(2);

    assertThat(keyLoader.getLoadedPublicKey("exampleKid", null).getPublicKey()).isSameAs(mockKey);
    assertThat(numberOfCalls.get()).isEqualTo(2);
  }

  @Test
  void shouldCountSources() {
    RSAPublicKey mockKey = Mockito.mock(RSAPublicKey.class);
    KeySource keySource =
        new KeySource() {
          @Override
          public List<LoadedPublicKey> loadKeysFromSource() throws KeyLoadFailedException {
            return singletonList(new LoadedPublicKey(null, null, mockKey, this, null, "RS256"));
          }
        };

    assertThat(keyLoader.getTotalNumberOfKeySources()).isZero();

    keyLoader.addKeySource(keySource);
    keyLoader.getKeysWithoutAnyId();
    keyLoader.getLoadedPublicKey("exampleKid", null);

    assertThat(keyLoader.getTotalNumberOfKeySources()).isEqualTo(1);
  }

  @Test
  void shouldCountAllKeys() {
    RSAPublicKey mockKey = Mockito.mock(RSAPublicKey.class);
    KeySource keySource =
        new KeySource() {
          @Override
          public List<LoadedPublicKey> loadKeysFromSource() throws KeyLoadFailedException {
            return Arrays.asList(
                new LoadedPublicKey(null, null, mockKey, this, null, "RS256"),
                new LoadedPublicKey(null, "exampleX5t", mockKey, this, null, "RS256"),
                new LoadedPublicKey("exampleKid", null, mockKey, this, null, "RS256"));
          }
        };

    assertThat(keyLoader.getTotalNumberOfKeys()).isZero();

    keyLoader.addKeySource(keySource);
    keyLoader.getKeysWithoutAnyId();
    keyLoader.getLoadedPublicKey("exampleKid", null);

    assertThat(keyLoader.getTotalNumberOfKeys()).isEqualTo(3);
  }

  @Test
  void shouldNotRemoveKeysIfReloadingFailed() {
    AtomicBoolean loaded = new AtomicBoolean();
    AtomicBoolean thrown = new AtomicBoolean();
    PublicKeyLoader rsaPublicKeyLoader = new PublicKeyLoader();
    KeySource keySource =
        new KeySource() {
          @Override
          public List<LoadedPublicKey> loadKeysFromSource() throws KeyLoadFailedException {
            if (loaded.getAndSet(true)) {
              thrown.set(true);
              throw new KeyLoadFailedException();
            }
            return singletonList(
                new LoadedPublicKey(
                    "the-kid", "the-x5t", Mockito.mock(RSAPublicKey.class), this, null, "RS256"));
          }
        };

    rsaPublicKeyLoader.addKeySource(keySource);
    assertThat(rsaPublicKeyLoader.getLoadedPublicKey("the-kid", null)).isNotNull();
    assertThat(rsaPublicKeyLoader.getLoadedPublicKey(null, "the-x5t")).isNotNull();

    assertThat(loaded).isTrue();
    assertThat(rsaPublicKeyLoader.getTotalNumberOfKeys()).isEqualTo(1);

    rsaPublicKeyLoader.getLoadedPublicKey("unknown-key-id", null);
    assertThat(rsaPublicKeyLoader.getTotalNumberOfKeys()).isEqualTo(1);

    rsaPublicKeyLoader.getLoadedPublicKey(null, "unknown-x5t");
    assertThat(rsaPublicKeyLoader.getTotalNumberOfKeys()).isEqualTo(1);

    assertThat(thrown).isTrue();
  }
}
