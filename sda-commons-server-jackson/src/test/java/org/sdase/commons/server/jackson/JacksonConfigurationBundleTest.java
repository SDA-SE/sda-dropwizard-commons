package org.sdase.commons.server.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.openapitools.jackson.dataformat.hal.HALLink;
import org.junit.Test;
import org.sdase.commons.server.jackson.test.ResourceWithLink;

import java.net.URI;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static com.google.common.truth.Truth.assertThat;
import static org.sdase.commons.server.jackson.test.ObjectMapperFactory.objectMapperFromBundle;


public class JacksonConfigurationBundleTest {

   @Test
   public void shouldAllowEmptyBean() throws Exception {
      ObjectMapper objectMapper = objectMapperFromBundle(JacksonConfigurationBundle.builder().build());

      String json = objectMapper.writeValueAsString(new Object());

      assertThat(json).isEqualTo("{}");
   }

   @Test
   public void shouldRenderSelfLink() throws Exception {

      ObjectMapper objectMapper = objectMapperFromBundle(JacksonConfigurationBundle.builder().build());
      HALLink link = new HALLink.Builder(URI.create("http://test/1")).build();
      ResourceWithLink resource = new ResourceWithLink().setSelf(link);

      String json = objectMapper.writeValueAsString(resource);

      assertThat(json).isEqualTo("{\"_links\":{\"self\":{\"href\":\"http://test/1\"}}}");

   }

   @Test
   public void shouldDisableHalSupport() throws Exception {

      ObjectMapper objectMapper = objectMapperFromBundle(JacksonConfigurationBundle.builder()
            .withoutHalSupport().build());
      HALLink link = new HALLink.Builder(URI.create("http://test/1")).build();
      ResourceWithLink resource = new ResourceWithLink().setSelf(link);

      String json = objectMapper.writeValueAsString(resource);

      assertThat(json).isEqualTo("{\"self\":{\"href\":\"http://test/1\"}}");

   }

   @Test
   public void shouldCustomizeObjectMapper() throws Exception {

      ObjectMapper objectMapper = objectMapperFromBundle(JacksonConfigurationBundle.builder()
            .withCustomization(om -> om.enable(INDENT_OUTPUT)).build());
      HALLink link = new HALLink.Builder(URI.create("http://test/1")).build();
      ResourceWithLink resource = new ResourceWithLink().setSelf(link);

      String json = objectMapper.writeValueAsString(resource);

      assertThat(json).isEqualTo("{" +System.lineSeparator()+
            "  \"_links\" : {"+System.lineSeparator()+
            "    \"self\" : {"+System.lineSeparator()+
            "      \"href\" : \"http://test/1\""+System.lineSeparator()+
            "    }"+System.lineSeparator()+
            "  }"+System.lineSeparator()+
            "}");

   }

}