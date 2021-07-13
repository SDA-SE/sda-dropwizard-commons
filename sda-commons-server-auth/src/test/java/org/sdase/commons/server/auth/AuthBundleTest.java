package org.sdase.commons.server.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sdase.commons.server.auth.config.AuthConfig;
import org.sdase.commons.server.auth.config.AuthConfigProvider;
import org.sdase.commons.server.auth.config.KeyLocation;
import org.sdase.commons.server.auth.config.KeyUriType;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class AuthBundleTest {

  public static final String LOGGED_TEXT =
      "The required issuer host name <{}> for the key <{}> "
          + "does not match to the key source uri host name <{}>.";

  private AuthBundle<Configuration> authBundle;
  private AuthConfig authConfig;

  @Mock private Appender<ILoggingEvent> mockAppender;
  @Mock private AuthConfigProvider<Configuration> authConfigProvider;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  Environment environment;

  @BeforeEach
  void setUp() {
    ch.qos.logback.classic.Logger logger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(AuthBundle.class.getName());
    logger.addAppender(mockAppender);

    authConfig = new AuthConfig();
    when(authConfigProvider.apply(any())).thenReturn(authConfig);
    authBundle =
        AuthBundle.builder()
            .withAuthConfigProvider(authConfigProvider)
            .withAnnotatedAuthorization()
            .build();
  }

  @Test
  void validateEmptyConfig() {
    authBundle.run(null, environment);
    verify(mockAppender, never()).doAppend(any());
  }

  @Test
  void validateJwksForDifferentContent() throws URISyntaxException {
    addKeyToConfig("http://otherhost/path", KeyUriType.JWKS, "http://localhost/auth");
    authBundle.run(null, environment);
    verify(mockAppender, times(1))
        .doAppend(
            ArgumentMatchers.argThat(
                argument -> {
                  assertThat(argument.getMessage()).isEqualTo(LOGGED_TEXT);
                  assertThat(argument.getLevel()).isEqualTo(Level.WARN);
                  return true;
                }));
  }

  @ParameterizedTest
  @MethodSource("defaultTestcaseData")
  void validateDefaultTestCase(String requiredIssuer, KeyUriType keyUriType, String keyLocationUri)
      throws URISyntaxException {
    addKeyToConfig(requiredIssuer, keyUriType, keyLocationUri);
    authBundle.run(null, environment);
    verify(mockAppender, never()).doAppend(any());
  }

  @ParameterizedTest
  @MethodSource("npeTestcaseData")
  void validateNPETestCase(String requiredIssuer, KeyUriType keyUriType, String keyLocationUri)
      throws URISyntaxException {
    addKeyToConfig(requiredIssuer, keyUriType, keyLocationUri);
    assertThatThrownBy(() -> authBundle.run(null, environment))
        .isInstanceOf(NullPointerException.class);
  }

  private static Stream<Arguments> defaultTestcaseData() {
    return Stream.of(
        Arguments.of("http://localhost/auth2/extra", KeyUriType.JWKS, "http://localhost/auth"),
        Arguments.of("http://localhost/auth", KeyUriType.JWKS, "http://localhost/auth"),
        Arguments.of(null, KeyUriType.JWKS, "http://localhost/auth"),
        Arguments.of("    ", KeyUriType.JWKS, "http://localhost/auth"),
        Arguments.of("nonURI", KeyUriType.JWKS, "http://localhost/auth"),
        Arguments.of(
            "http://localhost/auth2/extra", KeyUriType.OPEN_ID_DISCOVERY, "http://localhost/auth"),
        Arguments.of(
            "http://localhost/auth", KeyUriType.OPEN_ID_DISCOVERY, "http://localhost/auth"),
        Arguments.of(null, KeyUriType.OPEN_ID_DISCOVERY, "http://localhost/auth"),
        Arguments.of("    ", KeyUriType.OPEN_ID_DISCOVERY, "http://localhost/auth"),
        Arguments.of("nonURI", KeyUriType.OPEN_ID_DISCOVERY, "http://localhost/auth"));
  }

  private static Stream<Arguments> npeTestcaseData() {
    return Stream.of(
        Arguments.of("http://localhost/auth", KeyUriType.JWKS, null),
        Arguments.of("nonURI", KeyUriType.JWKS, null),
        Arguments.of(null, KeyUriType.JWKS, null),
        Arguments.of("http://localhost/auth", KeyUriType.OPEN_ID_DISCOVERY, null),
        Arguments.of("nonURI", KeyUriType.OPEN_ID_DISCOVERY, null),
        Arguments.of(null, KeyUriType.OPEN_ID_DISCOVERY, null));
  }

  @Test
  void validateDiscoveryForDifferentContent() throws URISyntaxException {
    addKeyToConfig("http://otherhost/path", KeyUriType.OPEN_ID_DISCOVERY, "http://localhost/auth");
    authBundle.run(null, environment);
    verify(mockAppender, times(1))
        .doAppend(
            ArgumentMatchers.argThat(
                argument -> {
                  assertThat(argument.getMessage()).isEqualTo(LOGGED_TEXT);
                  assertThat(argument.getLevel()).isEqualTo(Level.WARN);
                  return true;
                }));
  }

  private void addKeyToConfig(String requiredIssuer, KeyUriType keyUriType, String keyLocationUri)
      throws URISyntaxException {
    authConfig
        .getKeys()
        .add(
            new KeyLocation()
                .setType(keyUriType)
                .setLocation(keyLocationUri == null ? null : new URI(keyLocationUri))
                .setRequiredIssuer(requiredIssuer));
  }
}
