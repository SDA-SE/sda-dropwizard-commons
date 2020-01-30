package org.sdase.commons.server.auth.key;

import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Configuration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.math.BigInteger;
import java.util.List;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.auth.test.KeyProviderTestApp;

public class OpenIdProviderDiscoveryKeySourceIT {

  @ClassRule
  public static DropwizardAppRule<Configuration> DW =
      new DropwizardAppRule<>(
          KeyProviderTestApp.class,
          ResourceHelpers.resourceFilePath("test-config-key-provider.yaml"));

  @Test
  public void shouldLoadKeysFromHttp() {

    String location = "http://localhost:" + DW.getLocalPort() + "";
    OpenIdProviderDiscoveryKeySource keySource =
        new OpenIdProviderDiscoveryKeySource(location, DW.client());

    List<LoadedPublicKey> loadedPublicKeys = keySource.loadKeysFromSource();

    assertThat(loadedPublicKeys).hasSize(2);
    LoadedPublicKey loadedPublicKey = loadedPublicKeys.get(0);
    assertThat(loadedPublicKey.getKeySource()).isSameAs(keySource);
    assertThat(loadedPublicKey.getKid()).isEqualTo("rk82qxxLwy1wn6KTfAcyosSvwJ3uanZdChAvQYynq00");
    assertThatLoadedKeyContainsPublicKey(loadedPublicKey);
  }

  @Test(expected = KeyLoadFailedException.class)
  public void shouldThrowKeyLoadFailedExceptionOnFailure() {
    String location = "http://localhost:" + DW.getLocalPort() + "/test";
    OpenIdProviderDiscoveryKeySource keySource =
        new OpenIdProviderDiscoveryKeySource(location, DW.client());

    keySource.loadKeysFromSource();
  }

  @Test(expected = KeyLoadFailedException.class)
  public void shouldThrowKeyLoadFailedExceptionOnConnectionError() {
    String location = "http://unknownhost/test";
    OpenIdProviderDiscoveryKeySource keySource =
        new OpenIdProviderDiscoveryKeySource(location, DW.client());

    keySource.loadKeysFromSource();
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
