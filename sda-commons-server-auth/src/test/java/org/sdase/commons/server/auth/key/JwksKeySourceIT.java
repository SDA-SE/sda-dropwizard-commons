package org.sdase.commons.server.auth.key;

import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Configuration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.auth.test.KeyProviderTestApp;

public class JwksKeySourceIT {

  @ClassRule
  public static DropwizardAppRule<Configuration> DW =
      new DropwizardAppRule<>(
          KeyProviderTestApp.class,
          ResourceHelpers.resourceFilePath("test-config-key-provider.yaml"));

  @Test
  public void shouldLoadKeysFromHttp() {

    String location = "http://localhost:" + DW.getLocalPort() + "/jwks";
    JwksKeySource keySource = new JwksKeySource(location, DW.client());

    List<LoadedPublicKey> loadedPublicKeys = keySource.loadKeysFromSource();

    assertThat(loadedPublicKeys).hasSize(2);
    LoadedPublicKey loadedPublicKey = loadedPublicKeys.get(0);
    assertThat(loadedPublicKey.getKeySource()).isSameAs(keySource);
    assertThat(loadedPublicKey.getKid()).isEqualTo("rk82qxxLwy1wn6KTfAcyosSvwJ3uanZdChAvQYynq00");
    assertThatLoadedKeyContainsPublicKey(loadedPublicKey);
  }

  @Test
  public void shouldLoadKeyWithoutAlg() {

    String location = "http://localhost:" + DW.getLocalPort() + "/jwks";
    JwksKeySource keySource = new JwksKeySource(location, DW.client());

    List<LoadedPublicKey> loadedPublicKeys = keySource.loadKeysFromSource();

    assertThat(loadedPublicKeys)
        .extracting(LoadedPublicKey::getKid)
        .contains("uk82qxxLwy1wn6KTfAcyosSvwJ3uanZdChAvQYynq00");
  }

  @Test
  public void shouldNotLoadKeyWithWrongKeyType() {

    String location = "http://localhost:" + DW.getLocalPort() + "/jwks";
    JwksKeySource keySource = new JwksKeySource(location, DW.client());

    List<LoadedPublicKey> loadedPublicKeys = keySource.loadKeysFromSource();

    assertThat(loadedPublicKeys)
        .extracting(LoadedPublicKey::getKid)
        .doesNotContain("tk82qxxLwy1wn6KTfAcyosSvwJ3uanZdChAvQYynq00");
  }

  @Test
  public void shouldNotLoadKeyWithWrongAlg() {

    String location = "http://localhost:" + DW.getLocalPort() + "/jwks";
    JwksKeySource keySource = new JwksKeySource(location, DW.client());

    List<LoadedPublicKey> loadedPublicKeys = keySource.loadKeysFromSource();

    assertThat(loadedPublicKeys)
        .extracting(LoadedPublicKey::getKid)
        .doesNotContain("pk82qxxLwy1wn6KTfAcyosSvwJ3uanZdChAvQYynq00");
  }

  @Test
  public void shouldConsiderSameKeyLoadedTwiceAsEqual() {

    String location = "http://localhost:" + DW.getLocalPort() + "/jwks";
    JwksKeySource keySource = new JwksKeySource(location, DW.client());

    LoadedPublicKey key1 = keySource.loadKeysFromSource().get(0);
    LoadedPublicKey key2 = keySource.loadKeysFromSource().get(0);

    assertThat(key1).isNotSameAs(key2);
    assertThat(key1.equals(key2)).isTrue(); // test equals with custom impl
    assertThat(key1.getPublicKey()).isNotSameAs(key2.getPublicKey());
  }

  @Test(expected = KeyLoadFailedException.class)
  public void shouldFailWithKeyLoadFailedException() {
    String location = "http://localhost:" + DW.getLocalPort() + "/invalid-path";
    new JwksKeySource(location, DW.client()).loadKeysFromSource();
  }

  @Test(expected = KeyLoadFailedException.class)
  public void shouldFailWithKeyLoadFailedExceptionOnTimeout() {
    String location = "http://unknownhost/invalid-path";
    new JwksKeySource(location, DW.client()).loadKeysFromSource();
  }

  @Test
  public void shouldNotFailOnReload() {
    String location = "http://localhost:" + DW.getLocalPort() + "/invalid-path";
    Optional<List<LoadedPublicKey>> keys =
        new JwksKeySource(location, DW.client()).reloadKeysFromSource();
    assertThat(keys).isNotPresent();
  }

  private void assertThatLoadedKeyContainsPublicKey(LoadedPublicKey loadedPublicKey) {
    assertThat(loadedPublicKey.getPublicKey().getPublicExponent())
        .isEqualTo(new BigInteger("65537"));
    assertThat(loadedPublicKey.getPublicKey().getModulus())
        .isEqualTo(
            new BigInteger(
                "17537337770040194942919376835168802204646992470189073346292726680400174965130569492041958099763709556176841866525518878757501695295550867352207943032339433648136894460677492702293121619351540805764519231404975145580763406528201037441400852804277688599073671681299317742372001360551479125891210208047321905692537757022931808766479030891562871228210959536377544024035624115595277043081914911093929291636215236145876290280065235760159331468134303098491126397868806091868415470953841017816260225884166147788023540291440305396346794067794701203502009854410080784834782926947013108369449294441715521316665864950676584937289"));
  }
}
