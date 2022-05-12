package org.sdase.commons.server.auth.key;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.dropwizard.Configuration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECPoint;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.sdase.commons.server.auth.test.KeyProviderTestApp;

class PemKeySourceTest {

  @RegisterExtension
  static final DropwizardAppExtension<Configuration> DW =
      new DropwizardAppExtension<>(KeyProviderTestApp.class);

  @ParameterizedTest
  @CsvSource({"/rsa-key.pem, RS256", "/ec-key.pem, ES256"})
  void shouldLoadPemKeyFromHttp(String endpoint, String alg) {

    String location = "http://localhost:" + DW.getLocalPort() + endpoint;
    PemKeySource pemKeySource = new PemKeySource("exampleHttp", alg, URI.create(location), null);

    List<LoadedPublicKey> loadedPublicKeys = pemKeySource.loadKeysFromSource();

    assertThat(loadedPublicKeys).hasSize(1);
    LoadedPublicKey loadedPublicKey = loadedPublicKeys.get(0);
    assertThat(loadedPublicKey.getKeySource()).isSameAs(pemKeySource);
    assertThat(loadedPublicKey.getKid()).isEqualTo("exampleHttp");
    assertThatLoadedKeyContainsPublicKey(loadedPublicKey);
  }

  @ParameterizedTest
  @CsvSource({"rsa-example.pem, RS256", "ec-example.pem, ES256"})
  void shouldLoadPemKeyFromCertificateFile(String certificateFile, String alg) {
    PemKeySource pemKeySource =
        new PemKeySource(
            null, alg, new File(ResourceHelpers.resourceFilePath(certificateFile)).toURI(), null);

    List<LoadedPublicKey> loadedPublicKeys = pemKeySource.loadKeysFromSource();

    assertThat(loadedPublicKeys).hasSize(1);
    LoadedPublicKey loadedPublicKey = loadedPublicKeys.get(0);
    assertThat(loadedPublicKey.getKeySource()).isSameAs(pemKeySource);
    assertThat(loadedPublicKey.getKid()).isNull();
    assertThatLoadedKeyContainsPublicKey(loadedPublicKey);
  }

  @ParameterizedTest
  @ValueSource(strings = {"rsa-example-public-key.pem", "ec-example-public-key.pem"})
  void shouldLoadPemKeyFromPublicKeyFile(String publicKeyFile) {
    final String resourceFilePath = ResourceHelpers.resourceFilePath(publicKeyFile);
    PemKeySource pemKeySource =
        new PemKeySource(null, "RS256", new File(resourceFilePath).toURI(), null);

    List<LoadedPublicKey> loadedPublicKeys = pemKeySource.loadKeysFromSource();

    assertThat(loadedPublicKeys).hasSize(1);
    LoadedPublicKey loadedPublicKey = loadedPublicKeys.get(0);
    assertThat(loadedPublicKey.getKeySource()).isSameAs(pemKeySource);
    assertThat(loadedPublicKey.getKid()).isNull();
    assertThatLoadedKeyContainsPublicKey(loadedPublicKey);
  }

  @Test
  void shouldNotFailWithIOException() {
    File unknownFile = new File("DOES_NOT_EXIST.pem");
    PemKeySource pemKeySource = new PemKeySource(null, null, unknownFile.toURI(), null);

    assertThatExceptionOfType(KeyLoadFailedException.class)
        .isThrownBy(pemKeySource::loadKeysFromSource)
        .withCauseInstanceOf(IOException.class);
  }

  @Test
  void shouldNotFailWithNPE() {
    PemKeySource pemKeySource = new PemKeySource(null, null, null, null);

    assertThatExceptionOfType(KeyLoadFailedException.class)
        .isThrownBy(pemKeySource::loadKeysFromSource)
        .withCauseInstanceOf(NullPointerException.class);
  }

  private void assertThatLoadedKeyContainsPublicKey(LoadedPublicKey loadedPublicKey) {
    PublicKey publicKey = loadedPublicKey.getPublicKey();

    assertThat(publicKey)
        .withFailMessage("Not supported public key was loaded")
        .isInstanceOfAny(ECPublicKey.class, RSAPublicKey.class);

    if (publicKey instanceof RSAPublicKey) {
      assertThat(((RSAPublicKey) publicKey).getPublicExponent()).isEqualTo(new BigInteger("65537"));
      assertThat(((RSAPublicKey) publicKey).getModulus())
          .isEqualTo(
              new BigInteger(
                  "23278561008993559116324625988982470241312426956889346658405504678520644353694417096769495439990457626040214813030073192774164886177036082957412916823253078715836599659671998742580694113788009114660385412566349874736278693084819439968807941680965372239209505314975641627780628932505809153680703508450842086027900402475013187492167517569707571345375416089245182356787702312816149058026193158312146038158019813447205810433184619008248223295213470806341823186239417071266118809633344884486578155992325640138689812110143272054614608642914772652104720765422616303828138891725285516030216809064067106806135514473091101324387"));
    } else if (publicKey instanceof ECPublicKey) {
      assertThat(((ECPublicKey) publicKey).getW())
          .extracting(ECPoint::getAffineX, ECPoint::getAffineY)
          .containsExactly(
              new BigInteger(
                  1, Base64.getUrlDecoder().decode("COiQdbbJzZz1M3b8TDE7VOkm3moaW9hWMv8t4bZ2v8k")),
              new BigInteger(
                  1, Base64.getUrlDecoder().decode("v3QzLHuDP0iNLuHh1ajcS9yXwfwVStFip9TtIP8y2CM")));
    }
  }
}
