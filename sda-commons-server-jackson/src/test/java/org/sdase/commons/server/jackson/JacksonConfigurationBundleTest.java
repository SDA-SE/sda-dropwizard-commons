package org.sdase.commons.server.jackson;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Configuration;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.openapitools.jackson.dataformat.hal.HALLink;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.jackson.test.ResourceWithLink;

class JacksonConfigurationBundleTest {

  @Test
  void shouldAllowEmptyBean() throws Exception {
    ObjectMapper objectMapper =
        objectMapperFromBundle(JacksonConfigurationBundle.builder().build());

    String json = objectMapper.writeValueAsString(new Object());

    assertThat(json).isEqualTo("{}");
  }

  @Test
  void shouldRenderSelfLink() throws Exception {

    ObjectMapper objectMapper =
        objectMapperFromBundle(JacksonConfigurationBundle.builder().build());
    HALLink link = new HALLink.Builder(URI.create("http://test/1")).build();
    ResourceWithLink resource = new ResourceWithLink().setSelf(link);

    String json = objectMapper.writeValueAsString(resource);

    assertThat(json).isEqualTo("{\"_links\":{\"self\":{\"href\":\"http://test/1\"}}}");
  }

  @Test
  void shouldDisableHalSupport() throws Exception {

    ObjectMapper objectMapper =
        objectMapperFromBundle(JacksonConfigurationBundle.builder().withoutHalSupport().build());
    HALLink link = new HALLink.Builder(URI.create("http://test/1")).build();
    ResourceWithLink resource = new ResourceWithLink().setSelf(link);

    String json = objectMapper.writeValueAsString(resource);

    assertThat(json).isEqualTo("{\"self\":{\"href\":\"http://test/1\"}}");
  }

  @Test
  void shouldCustomizeObjectMapper() throws Exception {

    ObjectMapper objectMapper =
        objectMapperFromBundle(
            JacksonConfigurationBundle.builder()
                .withCustomization(om -> om.enable(INDENT_OUTPUT))
                .build());
    HALLink link = new HALLink.Builder(URI.create("http://test/1")).build();
    ResourceWithLink resource = new ResourceWithLink().setSelf(link);

    String json = objectMapper.writeValueAsString(resource);

    assertThat(json)
        .isEqualTo(
            "{"
                + System.lineSeparator()
                + "  \"_links\" : {"
                + System.lineSeparator()
                + "    \"self\" : {"
                + System.lineSeparator()
                + "      \"href\" : \"http://test/1\""
                + System.lineSeparator()
                + "    }"
                + System.lineSeparator()
                + "  }"
                + System.lineSeparator()
                + "}");
  }

  /**
   * @param jacksonConfigurationBundle the bundle
   * @return the {@link ObjectMapper} as it would be created in a Dropwizard App using the given
   *     {@code jacksonConfigurationBundle}.
   */
  private static ObjectMapper objectMapperFromBundle(
      JacksonConfigurationBundle jacksonConfigurationBundle) {
    AtomicReference<ObjectMapper> omRef = new AtomicReference<>();

    // ensure that the ObjectMapper to test is created as it would in a real App
    Bootstrap bootstrapMock = mock(Bootstrap.class, RETURNS_DEEP_STUBS);
    doAnswer(
            invocation -> {
              omRef.set(invocation.getArgument(0));
              return null;
            })
        .when(bootstrapMock)
        .setObjectMapper(any(ObjectMapper.class));
    when(bootstrapMock.getObjectMapper()).thenAnswer(invocation -> omRef.get());
    Environment environmentMock = mock(Environment.class, RETURNS_DEEP_STUBS);
    when(environmentMock.getObjectMapper()).thenAnswer(invocation -> omRef.get());
    Configuration configurationMock = mock(Configuration.class, RETURNS_DEEP_STUBS);
    DefaultServerFactory defaultServerFactoryMock = mock(DefaultServerFactory.class);
    when(configurationMock.getServerFactory()).thenReturn(defaultServerFactoryMock);

    jacksonConfigurationBundle.initialize(bootstrapMock);
    jacksonConfigurationBundle.run(configurationMock, environmentMock);

    // we must overwrite Dropwizard's ObjectMapper as early as possible
    verify(bootstrapMock, times(1)).setObjectMapper(omRef.get());
    // we should not use Dropwizard's ObjectMapper as unwanted Modules can't be removed
    verify(bootstrapMock, never()).getObjectMapper();
    // we should get the "default" ObjectMapper for customization in run()
    verify(environmentMock, times(1)).getObjectMapper();
    // we must disable default exception mappers because they are not properly overwritten in a CDI
    // context
    verify(defaultServerFactoryMock, times(1)).setRegisterDefaultExceptionMappers(Boolean.FALSE);

    return omRef.get();
  }
}
