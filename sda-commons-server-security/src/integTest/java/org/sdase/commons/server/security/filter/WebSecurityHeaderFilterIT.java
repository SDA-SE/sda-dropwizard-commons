package org.sdase.commons.server.security.filter;

import io.dropwizard.Configuration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sdase.commons.server.security.test.SecurityTestApp;

import javax.ws.rs.core.Response;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class WebSecurityHeaderFilterIT {

   @ClassRule
   public static final DropwizardAppRule<Configuration> DW = new DropwizardAppRule<>(
         SecurityTestApp.class,
         ResourceHelpers.resourceFilePath("test-config-no-settings.yaml")
   );

   private String requiredHeaderName;
   private String requiredHeaderValue;

   public WebSecurityHeaderFilterIT(String requiredHeaderName, String requiredHeaderValue) {
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
            new Object[] {"Content-Security-Policy", String.join("; ", // NOSONAR
                  asList(
                        "default-src 'self'",
                        "script-src 'self'",
                        "img-src 'self'",
                        "style-src 'self'",
                        "font-src 'self'",
                        "frame-src 'none'",
                        "object-src 'none'"
                  )
            )}
      );
   }

   @Test
   public void receiveDefinedHeader() {
      Response response = DW.client().target("http://localhost:" + DW.getLocalPort()).path("header").request().get();
      assertThat(response.getHeaders().get(requiredHeaderName)).containsExactly(requiredHeaderValue);
   }

   @Test
   public void allowOverwriteOfHeader() {
      Response response = DW.client().target("http://localhost:" + DW.getLocalPort()).path("header")
            .queryParam("name", requiredHeaderName)
            .queryParam("value", "CUSTOM_VALUE")
            .request().get();
      assertThat(response.getHeaders().get(requiredHeaderName)).containsExactly("CUSTOM_VALUE");
   }

}
