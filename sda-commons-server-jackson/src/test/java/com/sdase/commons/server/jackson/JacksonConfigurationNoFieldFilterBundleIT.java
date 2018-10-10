package com.sdase.commons.server.jackson;

import com.sdase.commons.server.jackson.test.JacksonConfigurationNoFieldFilterTestApp;
import com.sdase.commons.server.jackson.test.JacksonConfigurationTestApp;
import com.sdase.commons.server.jackson.test.JacksonConfigurationTestAppConfig;
import com.sdase.commons.server.jackson.test.PersonResource;
import io.dropwizard.Configuration;
import io.dropwizard.server.ServerFactory;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.eclipse.jetty.server.Server;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.GenericType;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

public class JacksonConfigurationNoFieldFilterBundleIT {

   @ClassRule
   public static final DropwizardAppRule<Configuration> DW = new DropwizardAppRule<>(
         JacksonConfigurationNoFieldFilterTestApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

   @Test
   public void shouldGetJohnDoe() {
      PersonResource johnny = DW.client()
            .target("http://localhost:" + DW.getLocalPort()).path("/jdoe")
            .request(APPLICATION_JSON)
            .get(PersonResource.class);

      assertThat(johnny)
            .extracting(
                  p -> p.getSelf().getHref(),
                  PersonResource::getFirstName,
                  PersonResource::getLastName,
                  PersonResource::getNickName
            )
            .containsExactly(
                  "http://localhost:" + DW.getLocalPort() + "/jdoe",
                  "John",
                  "Doe",
                  "Johnny"
            );
   }

   @Test
   public void shouldIgnoreFieldSelection() {
      Map<String, Object> johnny = DW.client()
            .target("http://localhost:" + DW.getLocalPort()).path("/jdoe")
            .queryParam("fields", "nickName")
            .request(APPLICATION_JSON)
            .get(new GenericType<Map<String, Object>>() {});

      assertThat(johnny)
            .containsKeys("_links", "firstName", "lastName", "nickName");
   }

   @Test
   public void shouldNotFilterField() {
      PersonResource johnny = DW.client()
            .target("http://localhost:" + DW.getLocalPort()).path("/jdoe")
            .queryParam("fields", "nickName")
            .request(APPLICATION_JSON)
            .get(PersonResource.class);

      assertThat(johnny)
            .extracting(
                  p -> p.getSelf().getHref(),
                  PersonResource::getFirstName,
                  PersonResource::getLastName,
                  PersonResource::getNickName
            )
            .containsExactly(
                  "http://localhost:" + DW.getLocalPort() + "/jdoe",
                  "John",
                  "Doe",
                  "Johnny"
            );
   }

   @Test
   public void shouldNotFilterFieldsByMultipleParams() {
      PersonResource johnny = DW.client()
            .target("http://localhost:" + DW.getLocalPort()).path("/jdoe")
            .queryParam("fields", "firstName")
            .queryParam("fields", "lastName")
            .request(APPLICATION_JSON)
            .get(PersonResource.class);

      assertThat(johnny)
            .extracting(
                  p -> p.getSelf().getHref(),
                  PersonResource::getFirstName,
                  PersonResource::getLastName,
                  PersonResource::getNickName
            )
            .containsExactly(
                  "http://localhost:" + DW.getLocalPort() + "/jdoe",
                  "John",
                  "Doe",
                  "Johnny"
            );
   }

   @Test
   public void shouldNotFilterFieldsBySingleParams() {
      PersonResource johnny = DW.client()
            .target("http://localhost:" + DW.getLocalPort()).path("/jdoe")
            .queryParam("fields", "firstName, lastName")
            .request(APPLICATION_JSON)
            .get(PersonResource.class);

      assertThat(johnny)
            .extracting(
                  p -> p.getSelf().getHref(),
                  PersonResource::getFirstName,
                  PersonResource::getLastName,
                  PersonResource::getNickName
            )
            .containsExactly(
                  "http://localhost:" + DW.getLocalPort() + "/jdoe",
                  "John",
                  "Doe",
                  "Johnny"
            );
   }
}
