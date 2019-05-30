package org.sdase.commons.server.auth.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import java.util.Optional;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.sdase.commons.server.auth.JwtPrincipal;
import org.sdase.commons.server.auth.error.JwtAuthException;
import org.sdase.commons.server.auth.filter.JwtAuthFilter.Builder;

public class JwtAuthFilterTest {
   @Mock
   ContainerRequestContext requestContext;
   @Mock
   Authenticator<Optional<String>, JwtPrincipal> authenticator;

   @Captor
   ArgumentCaptor<Optional<String>> credentialsCaptor;

   @Rule
   public MockitoRule mockitoRule = MockitoJUnit.rule();

   @Test(expected = JwtAuthException.class)
   public void throwsOnDefaultEmpty() throws AuthenticationException {
      // given
      MultivaluedStringMap headers = new MultivaluedStringMap();

      when(requestContext.getHeaders()).thenReturn(headers);
      when(authenticator.authenticate(credentialsCaptor.capture())).thenReturn(Optional.empty());

      JwtAuthFilter authFilter = new Builder<JwtPrincipal>().setAuthenticator(authenticator).buildAuthFilter();

      // when
      authFilter.filter(requestContext);
   }

   @Test
   public void shouldAcceptOnDefaultPayload() throws AuthenticationException {
      // given
      MultivaluedStringMap headers = new MultivaluedStringMap();
      headers.add(HttpHeaders.AUTHORIZATION, "Bearer MY_JWT");

      when(requestContext.getHeaders()).thenReturn(headers);
      when(authenticator.authenticate(credentialsCaptor.capture()))
            .thenReturn(Optional.of(JwtPrincipal.emptyPrincipal()));

      JwtAuthFilter authFilter = new Builder<JwtPrincipal>().setAuthenticator(authenticator).buildAuthFilter();

      // when
      authFilter.filter(requestContext);

      // then
      assertThat(credentialsCaptor.getValue()).contains("MY_JWT");
   }

   @Test
   public void shouldAcceptOnAnonymousEmpty() throws AuthenticationException {
      // given
      MultivaluedStringMap headers = new MultivaluedStringMap();

      when(requestContext.getHeaders()).thenReturn(headers);
      when(authenticator.authenticate(credentialsCaptor.capture())).thenReturn(Optional.empty());

      JwtAuthFilter authFilter = new Builder<JwtPrincipal>()
            .setAcceptAnonymous(true)
            .setAuthenticator(authenticator)
            .buildAuthFilter();

      // when
      authFilter.filter(requestContext);

      // then
      assertThat(credentialsCaptor.getValue()).isEmpty();
   }

   @Test
   public void shouldAcceptOnAnonymousPayload() throws AuthenticationException {
      // given
      MultivaluedStringMap headers = new MultivaluedStringMap();
      headers.add(HttpHeaders.AUTHORIZATION, "Bearer MY_JWT");

      when(requestContext.getHeaders()).thenReturn(headers);
      when(authenticator.authenticate(credentialsCaptor.capture()))
            .thenReturn(Optional.of(JwtPrincipal.emptyPrincipal()));

      JwtAuthFilter authFilter = new Builder<JwtPrincipal>()
            .setAcceptAnonymous(true)
            .setAuthenticator(authenticator)
            .buildAuthFilter();

      // when
      authFilter.filter(requestContext);

      // then
      assertThat(credentialsCaptor.getValue()).contains("MY_JWT");
   }
}
