package org.sdase.commons.server.opa.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.auth0.jwt.interfaces.Claim;
import java.security.Principal;
import java.util.Map;
import javax.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.opa.OpaJwtPrincipal;

class OpaJwtPrincipalFactoryTest {

  private SecurityContext securityContextMock = mock(SecurityContext.class);
  private OpaJwtPrincipalFactory opaJwtPrincipalFactory =
      new OpaJwtPrincipalFactory(securityContextMock);

  @Test
  void shouldProvideOpaJwtPrincipalFromSecurityContext() {

    Principal given = emptyOpaJwtPrincipal();
    when(securityContextMock.getUserPrincipal()).thenReturn(given);

    OpaJwtPrincipal actual = opaJwtPrincipalFactory.provide();

    assertThat(actual).isSameAs(given);
  }

  @Test
  void shouldSkipOtherTypeOfPrincipal() {

    Principal given = emptyGenericPrincipal();
    when(securityContextMock.getUserPrincipal()).thenReturn(given);

    OpaJwtPrincipal actual = opaJwtPrincipalFactory.provide();

    assertThat(actual).isNull();
  }

  @Test
  void shouldReturnNullIfNoPrincipal() {

    when(securityContextMock.getUserPrincipal()).thenReturn(null);

    OpaJwtPrincipal actual = opaJwtPrincipalFactory.provide();

    assertThat(actual).isNull();
  }

  private Principal emptyGenericPrincipal() {
    return () -> null;
  }

  private OpaJwtPrincipal emptyOpaJwtPrincipal() {
    return new OpaJwtPrincipal() {
      @Override
      public String getJwt() {
        return null;
      }

      @Override
      public Map<String, Claim> getClaims() {
        return null;
      }

      @Override
      public String getConstraints() {
        return null;
      }

      @Override
      public <T> T getConstraintsAsEntity(Class<T> resultType) {
        return null;
      }

      @Override
      public String getName() {
        return null;
      }
    };
  }
}
