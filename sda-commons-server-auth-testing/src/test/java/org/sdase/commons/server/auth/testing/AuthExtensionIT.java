package org.sdase.commons.server.auth.testing;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.WWW_AUTHENTICATE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.auth0.jwt.impl.PublicClaims;
import io.dropwizard.testing.ResourceHelpers;
import java.util.Date;
import java.util.Map;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.auth.testing.test.AuthTestApp;
import org.sdase.commons.server.auth.testing.test.AuthTestConfig;
import org.sdase.commons.server.testing.junit5.DropwizardAppExtension;

class AuthExtensionIT {

  @RegisterExtension public static final AuthExtension AUTH = AuthExtension.builder().build();

  @RegisterExtension
  public static final DropwizardAppExtension<AuthTestConfig> DW =
      new DropwizardAppExtension<>(
          AuthTestApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

  @Test
  void shouldAccessOpenEndPointWithoutToken() {
    Response response =
        createWebTarget()
            .path("/open") // NOSONAR
            .request(APPLICATION_JSON)
            .get();

    assertThat(response.getStatus()).isEqualTo(SC_OK);
    assertThat(response.readEntity(String.class)).isEqualTo("We are open."); // NOSONAR
  }

  @Test
  void shouldAccessOpenEndPointWithInvalidToken() {
    final String invalidTokenWithoutKid =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    Response response =
        createWebTarget()
            .path("/open")
            .request(APPLICATION_JSON)
            .header(AUTHORIZATION, "Bearer " + invalidTokenWithoutKid)
            .get();

    // No token checking at this point
    assertThat(response.getStatus()).isEqualTo(SC_OK);
    assertThat(response.readEntity(String.class)).isEqualTo("We are open.");
  }

  @Test
  void shouldBeUnauthorizedIfAccessedWithNonJwt() {
    Response response =
        createWebTarget()
            .path("/secure") // NOSONAR
            .request(APPLICATION_JSON)
            .header(AUTHORIZATION, "Bearer .")
            .get();

    assertThat(response.getStatus()).isEqualTo(SC_UNAUTHORIZED);
    assertThat(response.getHeaderString(WWW_AUTHENTICATE)).contains("Bearer"); // NOSONAR
    assertThat(response.getHeaderString(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON);
    Map<String, Object> stringObjectMap =
        response.readEntity(new GenericType<Map<String, Object>>() {});

    assertThat(stringObjectMap)
        .containsOnly(
            entry("title", "The token was expected to have 3 parts, but got 0."),
            entry("invalidParams", emptyList()));
  }

  @Test
  void shouldBeUnauthorizedIfAccessedWithTokenFromWrongIdp() {
    final String tokenWithUnknownKid =
        "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImNHV1RlQUJwWnYyN3RfWDFnTW92NEVlRWhEOXRBMWVhcUgzVzFmMXE4Y28ifQ.eyJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ0ZXN0In0.nHN-k_uvKNl8Nh5lXctQkL8KrWKggGiBQ-jaR0xIq_TAWBbhz5zkGXQTiNZwjPFOIcjyuL1xMCqzLPAKiI0Jy0hwOa4xcqukrWr4UwhKC50dnJiFqUgpGM0xLyT1D8JKdSNiVtYL0k-E5XCcpDEqOjHOG3Gw03VoZ0iRNeU2X49Rko8646l5j2g4QbuuOSn1a5G4ICMCAY7C6Vb55dgJtG_WAvkhFdBd_ShQEp_XfWJh6uq0E95_8yfzBx4UuK1Q-TLuWrXKxOlYNCuCH90NYG-3oF9w0gFtdXtYOFzPIEVIkU0Ra6sk_s0IInrEMD_3Q4fgE2PqOzqpuVaD_lHdAA";
    Response response =
        createWebTarget()
            .path("/secure") // NOSONAR
            .request(APPLICATION_JSON)
            .header(AUTHORIZATION, "Bearer " + tokenWithUnknownKid)
            .get();

    assertThat(response.getStatus()).isEqualTo(SC_UNAUTHORIZED);
    assertThat(response.getHeaderString(WWW_AUTHENTICATE)).contains("Bearer"); // NOSONAR
    assertThat(response.getHeaderString(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON);
    assertThat(response.readEntity(new GenericType<Map<String, Object>>() {}))
        .containsOnly(
            entry("title", "Could not verify JWT with the requested kid."),
            entry("invalidParams", emptyList()));
  }

  @Test
  void shouldAccessOpenEndPointWithToken() {
    Response response =
        createWebTarget()
            .path("/open")
            .request(APPLICATION_JSON)
            .headers(AUTH.auth().buildAuthHeader())
            .get();

    assertThat(response.getStatus()).isEqualTo(SC_OK);
    assertThat(response.readEntity(String.class)).isEqualTo("We are open.");
  }

  @Test
  void shouldNotAccessSecureEndPointWithoutToken() {
    Response response =
        createWebTarget()
            .path("/secure") // NOSONAR
            .request(APPLICATION_JSON)
            .get();

    assertThat(response.getStatus()).isEqualTo(SC_UNAUTHORIZED);
    assertThat(response.getHeaderString(WWW_AUTHENTICATE)).contains("Bearer"); // NOSONAR
    assertThat(response.getHeaderString(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON);
    assertThat(response.readEntity(new GenericType<Map<String, Object>>() {}))
        .containsOnly(
            entry("title", "Credentials are required to access this resource."),
            entry("invalidParams", emptyList()));
  }

  @Test
  void shouldNotAccessSecureEndPointWithInvalidToken() {
    Response response =
        createWebTarget()
            .path("/secure") // NOSONAR
            .request(APPLICATION_JSON)
            .header(
                AUTHORIZATION,
                "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
            .get();

    assertThat(response.getStatus()).isEqualTo(SC_UNAUTHORIZED);
    assertThat(response.getHeaderString(WWW_AUTHENTICATE)).contains("Bearer");
    assertThat(response.getHeaderString(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON);
    assertThat(response.readEntity(new GenericType<Map<String, Object>>() {}))
        .containsOnly(
            entry("title", "Could not verify JWT without kid."),
            entry("invalidParams", emptyList()));
  }

  @Test
  void shouldNotAccessSecureEndPointWithExpiredToken() {
    Response response =
        createWebTarget()
            .path("/secure") // NOSONAR
            .request(APPLICATION_JSON)
            .headers(AUTH.auth().addClaim(PublicClaims.EXPIRES_AT, new Date(0)).buildAuthHeader())
            .get();

    assertThat(response.getStatus()).isEqualTo(SC_UNAUTHORIZED);
    assertThat(response.getHeaderString(WWW_AUTHENTICATE)).contains("Bearer");
    assertThat(response.getHeaderString(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON);
    assertThat(response.readEntity(new GenericType<Map<String, Object>>() {}))
        .containsOnly(
            entry("title", "Verifying token failed"), entry("invalidParams", emptyList()));
  }

  @Test
  void shouldAccessSecureEndPointWithToken() {
    Response response =
        createWebTarget()
            .path("/secure")
            .request(APPLICATION_JSON)
            .headers(AUTH.auth().buildAuthHeader())
            .get();

    assertThat(response.getStatus()).isEqualTo(SC_OK);
    assertThat(response.readEntity(new GenericType<Map<String, String>>() {}))
        .contains(entry("iss", "AuthExtension"), entry("sub", "test"));
  }

  @Test
  void shouldGetClaimsFromSecureEndPointWithToken() {
    Response response =
        createWebTarget()
            .path("/secure")
            .request(APPLICATION_JSON)
            .headers(
                AUTH.auth()
                    .addClaim("test", "testClaim")
                    .addClaims(singletonMap("mapKey", "testClaimFromMap"))
                    .buildAuthHeader())
            .get();

    assertThat(response.getStatus()).isEqualTo(SC_OK);
    assertThat(response.readEntity(new GenericType<Map<String, String>>() {}))
        .contains(
            entry("iss", "AuthExtension"),
            entry("sub", "test"),
            entry("test", "testClaim"),
            entry("mapKey", "testClaimFromMap"));
  }

  private WebTarget createWebTarget() {
    return DW.client().target("http://localhost:" + DW.getLocalPort());
  }
}
