package org.sdase.commons.server.auth.key;

import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.ResourceHelpers;
import java.io.File;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

public class LoadedPublicKeyTest {

  @Test
  void shouldBeEqualWithDifferentKeyInstances() {

    URI pemKeyLocation = new File(ResourceHelpers.resourceFilePath("rsa-example.pem")).toURI();
    PemKeySource pemKeySource = new PemKeySource(null, null, pemKeyLocation, null);

    List<LoadedPublicKey> loadedPublicKeys1 = pemKeySource.loadKeysFromSource();
    List<LoadedPublicKey> loadedPublicKeys2 = pemKeySource.loadKeysFromSource();

    assertThat(loadedPublicKeys1).hasSize(1);
    LoadedPublicKey key1 = loadedPublicKeys1.get(0);
    LoadedPublicKey key2 = loadedPublicKeys2.get(0);
    assertThat(key1.equals(key2)).isTrue();
    assertThat(key1).isNotSameAs(key2);
  }
}
