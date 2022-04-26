package org.sdase.commons.server.auth.key;

import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.dropwizard.Configuration;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECPoint;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.auth.test.KeyProviderTestApp;

public class JwksKeySourceIT {

  @ClassRule
  public static DropwizardAppRule<Configuration> DW =
      new DropwizardAppRule<>(KeyProviderTestApp.class, null, randomPorts());

  @Test
  public void shouldLoadKeysFromHttp() {

    String location = "http://localhost:" + DW.getLocalPort() + "/jwks";
    JwksKeySource keySource = new JwksKeySource(location, DW.client(), null);

    List<LoadedPublicKey> loadedPublicKeys = keySource.loadKeysFromSource();

    assertThat(loadedPublicKeys)
        .hasSize(4)
        .extracting(LoadedPublicKey::getKeySource, LoadedPublicKey::getKid)
        .contains(
            tuple(keySource, "rk82qxxLwy1wn6KTfAcyosSvwJ3uanZdChAvQYynq00"),
            tuple(keySource, "jwx-hn1U7ho3DC-pWxliI1Oqo5-_hksg3TzPPfSsi68"),
            tuple(keySource, "46hiaXZquU4w-O6OIWUM1V6WXd4mbPoB6ZugPSrayR8"),
            tuple(keySource, "pk82qxxLwy1wn6KTfAcyosSvwJ3uanZdChAvQYynq00"));
  }

  @Test
  public void shouldLoadRsaPublicKey() {

    String location = "http://localhost:" + DW.getLocalPort() + "/jwks";
    JwksKeySource keySource = new JwksKeySource(location, DW.client(), null);

    Optional<PublicKey> rsaKey =
        keySource.loadKeysFromSource().stream()
            .filter(
                loadedPublicKey ->
                    loadedPublicKey.getKid().equals("rk82qxxLwy1wn6KTfAcyosSvwJ3uanZdChAvQYynq00"))
            .map(LoadedPublicKey::getPublicKey)
            .findFirst();

    assertThat(rsaKey).isPresent().get().isInstanceOf(RSAPublicKey.class);
    RSAPublicKey actual = (RSAPublicKey) rsaKey.get();
    assertThat(actual.getPublicExponent()).isEqualTo(new BigInteger("65537"));
    assertThat(actual.getModulus())
        .isEqualTo(
            new BigInteger(
                "17537337770040194942919376835168802204646992470189073346292726680400174965130569492041958099763709556176841866525518878757501695295550867352207943032339433648136894460677492702293121619351540805764519231404975145580763406528201037441400852804277688599073671681299317742372001360551479125891210208047321905692537757022931808766479030891562871228210959536377544024035624115595277043081914911093929291636215236145876290280065235760159331468134303098491126397868806091868415470953841017816260225884166147788023540291440305396346794067794701203502009854410080784834782926947013108369449294441715521316665864950676584937289"));
  }

  @Test
  public void shouldLoadEcPublicKey() {

    String location = "http://localhost:" + DW.getLocalPort() + "/jwks";
    JwksKeySource keySource = new JwksKeySource(location, DW.client(), null);

    Optional<PublicKey> rsaKey =
        keySource.loadKeysFromSource().stream()
            .filter(
                loadedPublicKey ->
                    loadedPublicKey.getKid().equals("46hiaXZquU4w-O6OIWUM1V6WXd4mbPoB6ZugPSrayR8"))
            .map(LoadedPublicKey::getPublicKey)
            .findFirst();

    assertThat(rsaKey).isPresent().get().isInstanceOf(ECPublicKey.class);
    ECPublicKey actual = (ECPublicKey) rsaKey.get();
    assertThat(actual.getW())
        .extracting(ECPoint::getAffineX, ECPoint::getAffineY)
        .containsExactly(
            new BigInteger(
                1,
                Base64.getUrlDecoder()
                    .decode(
                        "AKale3fvlHVbFM9t6LbWgEz7gpK_vMvhwitTJKBYopqEl9MQcEYhg2ZN-hRw28ggRugtzKKOATFrwrmqJB5mKzUP")),
            new BigInteger(
                1,
                Base64.getUrlDecoder()
                    .decode(
                        "ANnC-U8FHHzop3sa_TQxtl3IUqwsU36Bb8QF2HZr15a4esRZ4MTKgHA1r7DSmRq1iT-1soNQMzhHFV1oTck0Uenz")));
  }

  @Test
  public void shouldLoadNotKeyWithoutAlg() {

    String location = "http://localhost:" + DW.getLocalPort() + "/jwks";
    JwksKeySource keySource = new JwksKeySource(location, DW.client(), null);

    List<LoadedPublicKey> loadedPublicKeys = keySource.loadKeysFromSource();

    assertThat(loadedPublicKeys)
        .isNotEmpty()
        .extracting(LoadedPublicKey::getKid)
        .doesNotContain("uk82qxxLwy1wn6KTfAcyosSvwJ3uanZdChAvQYynq00");
  }

  @Test
  public void shouldNotLoadKeyWithWrongKeyType() {

    String location = "http://localhost:" + DW.getLocalPort() + "/jwks";
    JwksKeySource keySource = new JwksKeySource(location, DW.client(), null);

    List<LoadedPublicKey> loadedPublicKeys = keySource.loadKeysFromSource();

    assertThat(loadedPublicKeys)
        .extracting(LoadedPublicKey::getKid)
        .isNotEmpty()
        .doesNotContain("tk82qxxLwy1wn6KTfAcyosSvwJ3uanZdChAvQYynq00");
  }

  @Test
  public void shouldNotLoadKeyWithWrongAlg() {

    String location = "http://localhost:" + DW.getLocalPort() + "/jwks";
    JwksKeySource keySource = new JwksKeySource(location, DW.client(), null);

    List<LoadedPublicKey> loadedPublicKeys = keySource.loadKeysFromSource();

    assertThat(loadedPublicKeys)
        .extracting(LoadedPublicKey::getKid)
        .isNotEmpty()
        .doesNotContain("tk82qxxLwy1wn6KTfAcyosSvwJ3uanZdChAvQYynq00");
  }

  @Test
  public void shouldConsiderSameKeyLoadedTwiceAsEqual() {

    String location = "http://localhost:" + DW.getLocalPort() + "/jwks";
    JwksKeySource keySource = new JwksKeySource(location, DW.client(), null);

    LoadedPublicKey key1 = keySource.loadKeysFromSource().get(0);
    LoadedPublicKey key2 = keySource.loadKeysFromSource().get(0);

    assertThat(key1).isNotSameAs(key2);
    assertThat(key1.equals(key2)).isTrue(); // test equals with custom impl
    assertThat(key1.getPublicKey()).isNotSameAs(key2.getPublicKey());
  }

  @Test(expected = KeyLoadFailedException.class)
  public void shouldFailWithKeyLoadFailedException() {
    String location = "http://localhost:" + DW.getLocalPort() + "/invalid-path";
    new JwksKeySource(location, DW.client(), null).loadKeysFromSource();
  }

  @Test(expected = KeyLoadFailedException.class)
  public void shouldFailWithKeyLoadFailedExceptionOnTimeout() {
    String location = "http://unknownhost/invalid-path";
    new JwksKeySource(location, DW.client(), null).loadKeysFromSource();
  }

  @Test
  public void shouldNotFailOnReload() {
    String location = "http://localhost:" + DW.getLocalPort() + "/invalid-path";
    Optional<List<LoadedPublicKey>> keys =
        new JwksKeySource(location, DW.client(), null).reloadKeysFromSource();
    assertThat(keys).isNotPresent();
  }
}
