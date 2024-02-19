package org.sdase.commons.server.auth.key;

import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.math.BigInteger;
import java.net.URI;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECPoint;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.auth.AuthBundle;
import org.sdase.commons.server.auth.config.AuthConfig;
import org.sdase.commons.server.auth.config.KeyLocation;
import org.sdase.commons.server.auth.config.KeyUriType;
import org.sdase.commons.server.auth.test.KeyProviderTestApp;

class OpenIdProviderDiscoveryKeySourceIT {

  @RegisterExtension static OpenTelemetryExtension OTEL = OpenTelemetryExtension.create();

  @RegisterExtension
  static final DropwizardAppExtension<Configuration> DW =
      new DropwizardAppExtension<>(KeyProviderTestApp.class, null, randomPorts());

  @Test
  void shouldLoadKeysFromHttp() {

    String location = "http://localhost:" + DW.getLocalPort() + "";
    OpenIdProviderDiscoveryKeySource keySource =
        new OpenIdProviderDiscoveryKeySource(location, DW.client(), null);

    List<LoadedPublicKey> loadedPublicKeys = keySource.loadKeysFromSource();

    assertThat(loadedPublicKeys)
        .hasSize(4)
        .extracting(LoadedPublicKey::getKeySource, LoadedPublicKey::getKid)
        .contains(
            tuple(keySource, "rk82qxxLwy1wn6KTfAcyosSvwJ3uanZdChAvQYynq00"),
            tuple(keySource, "jwx-hn1U7ho3DC-pWxliI1Oqo5-_hksg3TzPPfSsi68"),
            tuple(keySource, "46hiaXZquU4w-O6OIWUM1V6WXd4mbPoB6ZugPSrayR8"),
            tuple(keySource, "pk82qxxLwy1wn6KTfAcyosSvwJ3uanZdChAvQYynq00"));

    Optional<PublicKey> actualKey =
        getLoadedKeyByKid(keySource, "rk82qxxLwy1wn6KTfAcyosSvwJ3uanZdChAvQYynq00");

    assertThat(actualKey).isPresent().get().isInstanceOf(RSAPublicKey.class);
    RSAPublicKey pk = (RSAPublicKey) actualKey.get();
    assertThat(pk.getPublicExponent()).isEqualTo(new BigInteger("65537"));
    assertThat(pk.getModulus())
        .isEqualTo(
            new BigInteger(
                "17537337770040194942919376835168802204646992470189073346292726680400174965130569492041958099763709556176841866525518878757501695295550867352207943032339433648136894460677492702293121619351540805764519231404975145580763406528201037441400852804277688599073671681299317742372001360551479125891210208047321905692537757022931808766479030891562871228210959536377544024035624115595277043081914911093929291636215236145876290280065235760159331468134303098491126397868806091868415470953841017816260225884166147788023540291440305396346794067794701203502009854410080784834782926947013108369449294441715521316665864950676584937289"));

    actualKey = getLoadedKeyByKid(keySource, "46hiaXZquU4w-O6OIWUM1V6WXd4mbPoB6ZugPSrayR8");

    assertThat(actualKey).isPresent().get().isInstanceOf(ECPublicKey.class);
    ECPublicKey pkEc = (ECPublicKey) actualKey.get();
    assertThat(pkEc.getW())
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

  private Optional<PublicKey> getLoadedKeyByKid(
      OpenIdProviderDiscoveryKeySource keySource, String kid) {
    return keySource.loadKeysFromSource().stream()
        .filter(loadedPublicKey -> loadedPublicKey.getKid().equals(kid))
        .map(LoadedPublicKey::getPublicKey)
        .findFirst();
  }

  @Test
  void shouldThrowKeyLoadFailedExceptionOnFailure() {
    String location = "http://localhost:" + DW.getLocalPort() + "/test";
    OpenIdProviderDiscoveryKeySource keySource =
        new OpenIdProviderDiscoveryKeySource(location, DW.client(), null);

    assertThrows(KeyLoadFailedException.class, keySource::loadKeysFromSource);
  }

  @Test
  void shouldThrowKeyLoadFailedExceptionOnConnectionError() {
    String location = "http://unknownhost/test";
    OpenIdProviderDiscoveryKeySource keySource =
        new OpenIdProviderDiscoveryKeySource(location, DW.client(), null);

    assertThrows(KeyLoadFailedException.class, keySource::loadKeysFromSource);
  }

  @Test
  void shouldLoadKeyWithTracing() {
    var authConfig = new AuthConfig();
    String oidcHost = "http://localhost:" + DW.getLocalPort();
    authConfig.setKeys(
        List.of(
            new KeyLocation()
                .setType(KeyUriType.OPEN_ID_DISCOVERY)
                .setLocation(URI.create(oidcHost))));
    var bundle =
        AuthBundle.builder()
            .withAuthConfigProvider(ConfigWithAuth::getAuthConfig)
            .withAnnotatedAuthorization()
            .withOpenTelemetry(OTEL.getOpenTelemetry())
            .build();
    bundle.initialize(mock(Bootstrap.class, RETURNS_DEEP_STUBS));
    var environmentMock = mock(Environment.class, RETURNS_DEEP_STUBS);
    when(environmentMock.getObjectMapper()).thenReturn(new ObjectMapper());
    bundle.run(new ConfigWithAuth(authConfig), environmentMock);
    await()
        .untilAsserted(
            () -> {
              var spans = OTEL.getSpans();
              assertThat(spans)
                  .hasSize(2)
                  .extracting(
                      s -> s.getAttributes().asMap().get(stringKey("url.full")), SpanData::getName)
                  .containsExactly(
                      tuple(oidcHost + "/.well-known/openid-configuration", "GET"),
                      tuple(oidcHost + "/jwks", "GET"));
            });
  }

  static class ConfigWithAuth extends Configuration {
    private final AuthConfig authConfig;

    public ConfigWithAuth(AuthConfig authConfig) {
      this.authConfig = authConfig;
    }

    public AuthConfig getAuthConfig() {
      return authConfig;
    }
  }
}
