package org.sdase.commons.server.security.filter;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Configuration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.Collection;
import javax.ws.rs.core.Response;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sdase.commons.server.security.test.SecurityWithFrontendTestApp;

@RunWith(Parameterized.class)
public class WebSecurityFrontendSupportHeaderFilterIT {

  @ClassRule
  public static final DropwizardAppRule<Configuration> DW =
      new DropwizardAppRule<>(
          SecurityWithFrontendTestApp.class,
          ResourceHelpers.resourceFilePath("test-config-no-settings.yaml"));

  private final String requiredHeaderName;
  private final String requiredHeaderValue;

  public WebSecurityFrontendSupportHeaderFilterIT(
      String requiredHeaderName, String requiredHeaderValue) {
    this.requiredHeaderName = requiredHeaderName;
    this.requiredHeaderValue = requiredHeaderValue;
  }

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(
        new Object[] {"X-Frame-Options", "DENY"},
        new Object[] {"X-Content-Type-Options", "nosniff"},
        new Object[] {"X-XSS-Protection", "1; mode=block"},
        new Object[] {"Referrer-Policy", "same-origin"},
        new Object[] {"X-Permitted-Cross-Domain-Policies", "none"},
        new Object[] {
          "Content-Security-Policy",
          String.join(
              "; ",
              asList(
                  "default-src 'self'",
                  "script-src 'self'",
                  "img-src 'self'",
                  "style-src 'self'",
                  "font-src 'self'",
                  "frame-src 'none'",
                  "object-src 'none'"))
        });
  }

  @Test
  public void receiveDefinedHeader() {
    Response response =
        DW.client().target("http://localhost:" + DW.getLocalPort()).path("header").request().get();
    assertThat(response.getHeaders().get(requiredHeaderName)).containsExactly(requiredHeaderValue);
  }

  @Test
  public void allowOverwriteOfHeader() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("header")
            .queryParam("name", requiredHeaderName)
            .queryParam("value", "CUSTOM_VALUE")
            .request()
            .get();
    assertThat(response.getHeaders().get(requiredHeaderName)).containsExactly("CUSTOM_VALUE");
  }
}
