package org.sdase.commons.server.auth.key;

import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.dropwizard.Configuration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.util.List;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.auth.test.KeyProviderTestApp;

public class PemKeySourceTest {

  @ClassRule
  public static DropwizardAppRule<Configuration> DW =
      new DropwizardAppRule<>(KeyProviderTestApp.class, null, randomPorts());

  @Test
  public void shouldLoadPemKeyFromHttp() {

    String location = "http://localhost:" + DW.getLocalPort() + "/key.pem";
    PemKeySource pemKeySource = new PemKeySource("exampleHttp", URI.create(location), null);

    List<LoadedPublicKey> loadedPublicKeys = pemKeySource.loadKeysFromSource();

    assertThat(loadedPublicKeys).hasSize(1);
    LoadedPublicKey loadedPublicKey = loadedPublicKeys.get(0);
    assertThat(loadedPublicKey.getKeySource()).isSameAs(pemKeySource);
    assertThat(loadedPublicKey.getKid()).isEqualTo("exampleHttp");
    assertThatLoadedKeyContainsPublicKey(loadedPublicKey);
  }

  @Test
  public void shouldLoadPemKeyFromCertificateFile() {
    PemKeySource pemKeySource =
        new PemKeySource(
            null, new File(ResourceHelpers.resourceFilePath("example.pem")).toURI(), null);

    List<LoadedPublicKey> loadedPublicKeys = pemKeySource.loadKeysFromSource();

    assertThat(loadedPublicKeys).hasSize(1);
    LoadedPublicKey loadedPublicKey = loadedPublicKeys.get(0);
    assertThat(loadedPublicKey.getKeySource()).isSameAs(pemKeySource);
    assertThat(loadedPublicKey.getKid()).isNull();
    assertThatLoadedKeyContainsPublicKey(loadedPublicKey);
  }

  @Test
  public void shouldLoadPemKeyFromPublicKeyFile() {
    final String resourceFilePath = ResourceHelpers.resourceFilePath("example-public-key.pem");
    PemKeySource pemKeySource = new PemKeySource(null, new File(resourceFilePath).toURI(), null);

    List<LoadedPublicKey> loadedPublicKeys = pemKeySource.loadKeysFromSource();

    assertThat(loadedPublicKeys).hasSize(1);
    LoadedPublicKey loadedPublicKey = loadedPublicKeys.get(0);
    assertThat(loadedPublicKey.getKeySource()).isSameAs(pemKeySource);
    assertThat(loadedPublicKey.getKid()).isNull();
    assertThatLoadedKeyContainsPublicKey(loadedPublicKey);
  }

  @Test
  public void shouldNotFailWithIOException() {
    File unknownFile = new File("DOES_NOT_EXIST.pem");
    PemKeySource pemKeySource = new PemKeySource(null, unknownFile.toURI(), null);

    assertThatExceptionOfType(KeyLoadFailedException.class)
        .isThrownBy(pemKeySource::loadKeysFromSource)
        .withCauseInstanceOf(IOException.class);
  }

  @Test
  public void shouldNotFailWithNPE() {
    PemKeySource pemKeySource = new PemKeySource(null, null, null);

    assertThatExceptionOfType(KeyLoadFailedException.class)
        .isThrownBy(pemKeySource::loadKeysFromSource)
        .withCauseInstanceOf(NullPointerException.class);
  }

  private void assertThatLoadedKeyContainsPublicKey(LoadedPublicKey loadedPublicKey) {
    assertThat(loadedPublicKey.getPublicKey().getPublicExponent())
        .isEqualTo(new BigInteger("65537"));
    assertThat(loadedPublicKey.getPublicKey().getModulus())
        .isEqualTo(
            new BigInteger(
                "23278561008993559116324625988982470241312426956889346658405504678520644353694417096769495439990457626040214813030073192774164886177036082957412916823253078715836599659671998742580694113788009114660385412566349874736278693084819439968807941680965372239209505314975641627780628932505809153680703508450842086027900402475013187492167517569707571345375416089245182356787702312816149058026193158312146038158019813447205810433184619008248223295213470806341823186239417071266118809633344884486578155992325640138689812110143272054614608642914772652104720765422616303828138891725285516030216809064067106806135514473091101324387"));
  }
}
