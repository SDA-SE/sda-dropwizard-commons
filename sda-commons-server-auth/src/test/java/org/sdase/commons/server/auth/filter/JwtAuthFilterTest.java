package org.sdase.commons.server.auth.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import java.util.Optional;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sdase.commons.server.auth.JwtPrincipal;
import org.sdase.commons.server.auth.error.JwtAuthException;
import org.sdase.commons.server.auth.filter.JwtAuthFilter.Builder;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {
  @Mock ContainerRequestContext requestContext;
  @Mock Authenticator<Optional<String>, JwtPrincipal> authenticator;

  @Captor ArgumentCaptor<Optional<String>> credentialsCaptor;

  @Test
  void throwsOnDefaultEmpty() throws AuthenticationException {
    // given
    MultivaluedStringMap headers = new MultivaluedStringMap();

    when(requestContext.getHeaders()).thenReturn(headers);
    when(authenticator.authenticate(credentialsCaptor.capture())).thenReturn(Optional.empty());

    JwtAuthFilter<JwtPrincipal> authFilter =
        new Builder<JwtPrincipal>().setAuthenticator(authenticator).buildAuthFilter();

    // when
    Assertions.assertThrows(
        JwtAuthException.class,
        () -> {
          authFilter.filter(requestContext);
        });
  }

  @Test
  void shouldAcceptOnDefaultPayload() throws AuthenticationException {
    // given
    MultivaluedStringMap headers = new MultivaluedStringMap();
    headers.add(HttpHeaders.AUTHORIZATION, "Bearer MY_JWT");

    when(requestContext.getHeaders()).thenReturn(headers);
    when(authenticator.authenticate(credentialsCaptor.capture()))
        .thenReturn(Optional.of(JwtPrincipal.emptyPrincipal()));

    JwtAuthFilter authFilter =
        new Builder<JwtPrincipal>().setAuthenticator(authenticator).buildAuthFilter();

    // when
    authFilter.filter(requestContext);

    // then
    assertThat(credentialsCaptor.getValue()).contains("MY_JWT");
  }

  @Test
  void shouldAcceptOnAnonymousEmpty() throws AuthenticationException {
    // given
    MultivaluedStringMap headers = new MultivaluedStringMap();

    when(requestContext.getHeaders()).thenReturn(headers);
    when(authenticator.authenticate(credentialsCaptor.capture())).thenReturn(Optional.empty());

    JwtAuthFilter authFilter =
        new Builder<JwtPrincipal>()
            .setAcceptAnonymous(true)
            .setAuthenticator(authenticator)
            .buildAuthFilter();

    // when
    authFilter.filter(requestContext);

    // then
    assertThat(credentialsCaptor.getValue()).isEmpty();
  }

  @Test
  void shouldAcceptOnAnonymousPayload() throws AuthenticationException {
    // given
    MultivaluedStringMap headers = new MultivaluedStringMap();
    headers.add(HttpHeaders.AUTHORIZATION, "Bearer MY_JWT");

    when(requestContext.getHeaders()).thenReturn(headers);
    when(authenticator.authenticate(credentialsCaptor.capture()))
        .thenReturn(Optional.of(JwtPrincipal.emptyPrincipal()));

    JwtAuthFilter authFilter =
        new Builder<JwtPrincipal>()
            .setAcceptAnonymous(true)
            .setAuthenticator(authenticator)
            .buildAuthFilter();

    // when
    authFilter.filter(requestContext);

    // then
    assertThat(credentialsCaptor.getValue()).contains("MY_JWT");
  }
}
