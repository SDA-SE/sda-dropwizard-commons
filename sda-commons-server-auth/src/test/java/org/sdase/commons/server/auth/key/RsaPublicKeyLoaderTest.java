package org.sdase.commons.server.auth.key;

import org.junit.Test;
import org.mockito.Mockito;

import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class RsaPublicKeyLoaderTest {

   private RsaPublicKeyLoader keyLoader = new RsaPublicKeyLoader();

   @Test
   public void shouldAddKeyWithoutKid() {
      RSAPublicKey mockKey = Mockito.mock(RSAPublicKey.class);
      KeySource keySource = new KeySource() {
         @Override
         public List<LoadedPublicKey> loadKeysFromSource() throws KeyLoadFailedException {
            return singletonList(new LoadedPublicKey(null, mockKey, this));
         }
      };

      keyLoader.addKeySource(keySource);

      assertThat(keyLoader.getKeysWithoutId()).containsExactly(mockKey);
      assertThat(keyLoader.getKey(null)).isNull();
   }

   @Test
   public void shouldAddKeyWithKid() {
      RSAPublicKey mockKey = Mockito.mock(RSAPublicKey.class);
      KeySource keySource = new KeySource() {
         @Override
         public List<LoadedPublicKey> loadKeysFromSource() throws KeyLoadFailedException {
            return singletonList(new LoadedPublicKey("exampleKid", mockKey, this));
         }
      };

      keyLoader.addKeySource(keySource);

      assertThat(keyLoader.getKeysWithoutId()).isEmpty();
      assertThat(keyLoader.getKey("exampleKid")).isSameAs(mockKey);
   }

   @Test
   public void shouldReloadIfKeyIsNotFoundAndRemoveOutdatedKeys() {
      RSAPublicKey mockKey = Mockito.mock(RSAPublicKey.class);
      RSAPublicKey newMockKey = Mockito.mock(RSAPublicKey.class);
      AtomicInteger numberOfCalls = new AtomicInteger();
      KeySource keySource = new KeySource() {
         @Override
         public List<LoadedPublicKey> loadKeysFromSource() throws KeyLoadFailedException {
            if (numberOfCalls.getAndIncrement() < 1) {
               return singletonList(new LoadedPublicKey("exampleKid", mockKey, this));
            }
            else {
               return singletonList(new LoadedPublicKey("newKid", newMockKey, this));
            }
         }
      };

      keyLoader.addKeySource(keySource);

      assertThat(keyLoader.getKey("exampleKid")).isSameAs(mockKey);
      assertThat(numberOfCalls.get()).isEqualTo(1);

      assertThat(keyLoader.getKey("exampleKid")).isSameAs(mockKey);
      // no reload for known key
      assertThat(numberOfCalls.get()).isEqualTo(1);

      assertThat(keyLoader.getKey("newKid")).isSameAs(newMockKey);
      assertThat(numberOfCalls.get()).isEqualTo(2);

      assertThat(keyLoader.getKey("exampleKid")).isNull();
      assertThat(numberOfCalls.get()).isEqualTo(3);

   }

   @Test
   public void shouldReloadIfKeyIsNotFound() {
      RSAPublicKey mockKey = Mockito.mock(RSAPublicKey.class);
      RSAPublicKey newMockKey = Mockito.mock(RSAPublicKey.class);
      AtomicInteger numberOfCalls = new AtomicInteger();
      KeySource keySource = new KeySource() {
         @Override
         public List<LoadedPublicKey> loadKeysFromSource() throws KeyLoadFailedException {
            if (numberOfCalls.getAndIncrement() < 1) {
               return singletonList(new LoadedPublicKey("exampleKid", mockKey, this));
            }
            else {
               return Arrays.asList(
                     new LoadedPublicKey("exampleKid", mockKey, this),
                     new LoadedPublicKey("newKid", newMockKey, this)
               );
            }
         }
      };

      keyLoader.addKeySource(keySource);

      assertThat(keyLoader.getKey("exampleKid")).isSameAs(mockKey);
      assertThat(numberOfCalls.get()).isEqualTo(1);

      assertThat(keyLoader.getKey("exampleKid")).isSameAs(mockKey);
      // no reload for known key
      assertThat(numberOfCalls.get()).isEqualTo(1);

      assertThat(keyLoader.getKey("newKid")).isSameAs(newMockKey);
      assertThat(numberOfCalls.get()).isEqualTo(2);

      assertThat(keyLoader.getKey("exampleKid")).isSameAs(mockKey);
      assertThat(numberOfCalls.get()).isEqualTo(2);

   }

   @Test
   public void shouldNotRemoveIfReloadFails() {
      RSAPublicKey mockKey = Mockito.mock(RSAPublicKey.class);
      AtomicInteger numberOfCalls = new AtomicInteger();
      KeySource keySource = new KeySource() {
         @Override
         public List<LoadedPublicKey> loadKeysFromSource() throws KeyLoadFailedException {
            if (numberOfCalls.getAndIncrement() < 1) {
               return singletonList(new LoadedPublicKey("exampleKid", mockKey, this));
            }
            else {
               throw new KeyLoadFailedException("Test error");
            }
         }
      };

      keyLoader.addKeySource(keySource);

      assertThat(keyLoader.getKey("exampleKid")).isSameAs(mockKey);
      assertThat(numberOfCalls.get()).isEqualTo(1);

      assertThat(keyLoader.getKey("exampleKid")).isSameAs(mockKey);
      // no reload for known key
      assertThat(numberOfCalls.get()).isEqualTo(1);

      assertThat(keyLoader.getKey("newKid")).isNull();
      assertThat(numberOfCalls.get()).isEqualTo(2);

      assertThat(keyLoader.getKey("exampleKid")).isSameAs(mockKey);
      assertThat(numberOfCalls.get()).isEqualTo(2);

   }

   @Test
   public void shouldCountSources() {
      RSAPublicKey mockKey = Mockito.mock(RSAPublicKey.class);
      KeySource keySource = new KeySource() {
         @Override
         public List<LoadedPublicKey> loadKeysFromSource() throws KeyLoadFailedException {
            return singletonList(new LoadedPublicKey(null, mockKey, this));
         }
      };

      assertThat(keyLoader.getTotalNumberOfKeySources()).isEqualTo(0);

      keyLoader.addKeySource(keySource);
      keyLoader.getKeysWithoutId();
      keyLoader.getKey("exampleKid");

      assertThat(keyLoader.getTotalNumberOfKeySources()).isEqualTo(1);
   }

   @Test
   public void shouldCountAllKeys() {
      RSAPublicKey mockKey = Mockito.mock(RSAPublicKey.class);
      KeySource keySource = new KeySource() {
         @Override
         public List<LoadedPublicKey> loadKeysFromSource() throws KeyLoadFailedException {
            return Arrays.asList(
                  new LoadedPublicKey(null, mockKey, this),
                  new LoadedPublicKey("exampleKid", mockKey, this)
            );
         }
      };

      assertThat(keyLoader.getTotalNumberOfKeys()).isEqualTo(0);

      keyLoader.addKeySource(keySource);
      keyLoader.getKeysWithoutId();
      keyLoader.getKey("exampleKid");

      assertThat(keyLoader.getTotalNumberOfKeys()).isEqualTo(2);
   }

   @Test
   public void shouldNotRemoveKeysIfReloadingFailed() {

   }
}