package org.sdase.commons.server.swagger;

import io.dropwizard.Configuration;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.jetty.setup.ServletEnvironment;
import io.dropwizard.server.AbstractServerFactory;
import io.dropwizard.server.ServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.models.Info;
import javax.servlet.FilterRegistration;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.net.InetAddress;
import java.util.Optional;

import static java.lang.String.join;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.STRICT_STUBS;

public class SwaggerBundleTest {

   private static final String VALID_TITLE = "VALID_TITLE";
   private static final String VALID_NAME = "VALID_NAME";
   private static final String VALID_EMAIL = "test@test.com";
   private static final String VALID_URL = "https://www.example.com";

   @Rule
   public MockitoRule rule = MockitoJUnit.rule().strictness(STRICT_STUBS);

   @Rule
   public ExpectedException expectedException = ExpectedException.none();

   @Mock
   private Bootstrap<?> bootstrap;

   @Mock
   private Configuration configuration;

   @Mock
   private Environment environment;

   @Mock
   private JerseyEnvironment jerseyEnvironment;

   @Mock
   private ServletEnvironment servletEnvironment;

   @Mock
   private FilterRegistration.Dynamic crossOriginFilter;

   @Mock
   private AbstractServerFactory serverFactory;

   @Test
   public void shouldThrowNullPointerExceptionWhenTitleIsNull() {
      expectedException.expect(NullPointerException.class);
      expectedException.expectMessage("title");

      SwaggerBundle.builder().withTitle(null);
   }

   @Test
   public void shouldThrowIllegalArgumentExceptionWhenTitleIsEmpty() {
      expectedException.expect(IllegalArgumentException.class);
      expectedException.expectMessage("title");

      SwaggerBundle.builder().withTitle("");
   }

   @Test
   public void shouldThrowNullPointerExceptionWhenVersionIsNull() {
      expectedException.expect(NullPointerException.class);
      expectedException.expectMessage("version");

      SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(getClass())
            .withVersion(null);
   }

   @Test
   public void shouldThrowIllegalArgumentExceptionWhenVersionIsEmpty() {
      expectedException.expect(IllegalArgumentException.class);
      expectedException.expectMessage("version");

      SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(getClass())
            .withVersion("");
   }

   @Test
   public void shouldThrowNullPointerExceptionWhenDescriptionIsNull() {
      expectedException.expect(NullPointerException.class);
      expectedException.expectMessage("description");

      SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(getClass())
            .withDescription(null);
   }

   @Test
   public void shouldThrowIllegalArgumentExceptionWhenDescriptionIsEmpty() {
      expectedException.expect(IllegalArgumentException.class);
      expectedException.expectMessage("description");

      SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(getClass())
            .withDescription("");
   }

   @Test
   public void shouldThrowNullPointerExceptionWhenTermsOfServiceUrlIsNull() {
      expectedException.expect(NullPointerException.class);
      expectedException.expectMessage("termsOfServiceUrl");

      SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(getClass())
            .withTermsOfServiceUrl(null);
   }

   @Test
   public void shouldThrowIllegalArgumentExceptionWhenTermsOfServiceUrlIsEmpty() {
      expectedException.expect(IllegalArgumentException.class);
      expectedException.expectMessage("termsOfServiceUrl");

      SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(getClass())
            .withTermsOfServiceUrl("");
   }

   @Test
   public void shouldThrowNullPointerExceptionWhenResourcePackageIsNull() {
      expectedException.expect(NullPointerException.class);
      expectedException.expectMessage("resourcePackage");

      SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackage(null);
   }

   @Test
   public void shouldThrowIllegalArgumentExceptionWhenResourcePackageClassIsNull() {
      expectedException.expect(IllegalArgumentException.class);
      expectedException.expectMessage("resourcePackage");

      SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackage("");
   }

   @Test
   public void shouldThrowNullPointerExceptionWhenContactNameIsNullOneArg() {
      expectedException.expect(NullPointerException.class);
      expectedException.expectMessage("name");

      SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(getClass())
            .withContact(null);
   }

   @Test
   public void shouldThrowIllegalArgumentExceptionWhenContactNameIsEmptyOneArg() {
      expectedException.expect(IllegalArgumentException.class);
      expectedException.expectMessage("name");

      SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(getClass())
            .withContact("");
   }

   @Test
   public void shouldThrowNullPointerExceptionWhenContactNameIsNullTwoArgs() {
      expectedException.expect(NullPointerException.class);
      expectedException.expectMessage("name");

      SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(getClass())
            .withContact(null, VALID_EMAIL);
   }

   @Test
   public void shouldThrowIllegalArgumentExceptionWhenContactNameIsEmptyTwoArgs() {
      expectedException.expect(IllegalArgumentException.class);
      expectedException.expectMessage("name");

      SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(getClass())
            .withContact("", VALID_EMAIL);
   }

   @Test
   public void shouldThrowNullPointerExceptionWhenContactNameIsNullThreeArgs() {
      expectedException.expect(NullPointerException.class);
      expectedException.expectMessage("name");

      SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(getClass())
            .withContact(null, VALID_EMAIL, VALID_URL);
   }

   @Test
   public void shouldThrowIllegalArgumentExceptionWhenContactNameIsEmptyThreeArgs() {
      expectedException.expect(IllegalArgumentException.class);
      expectedException.expectMessage("name");

      SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(getClass())
            .withContact("", VALID_EMAIL, VALID_URL);
   }

   @Test
   public void shouldThrowNullPointerExceptionWhenContactEmailIsNullTwoArgs() {
      expectedException.expect(NullPointerException.class);
      expectedException.expectMessage("email");

      SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(getClass())
            .withContact(VALID_NAME, null);
   }

   @Test
   public void shouldThrowIllegalArgumentExceptionWhenContactEmailIsEmptyTwoArgs() {
      expectedException.expect(IllegalArgumentException.class);
      expectedException.expectMessage("email");

      SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(getClass())
            .withContact(VALID_NAME, "");
   }

   @Test
   public void shouldThrowNullPointerExceptionWhenContactEmailIsNullThreeArgs() {
      expectedException.expect(NullPointerException.class);
      expectedException.expectMessage("email");

      SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(getClass())
            .withContact(VALID_NAME, null, VALID_URL);
   }

   @Test
   public void shouldThrowIllegalArgumentExceptionWhenContactEmailIsEmptyThreeArgs() {
      expectedException.expect(IllegalArgumentException.class);
      expectedException.expectMessage("email");

      SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(getClass())
            .withContact(VALID_NAME, "", VALID_URL);
   }

   @Test
   public void shouldThrowNullPointerExceptionWhenContactUrlIsNull() {
      expectedException.expect(NullPointerException.class);
      expectedException.expectMessage("url");

      SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(getClass())
            .withContact(VALID_NAME, VALID_EMAIL, null);
   }

   @Test
   public void shouldThrowIllegalArgumentExceptionWhenContactUrlIsEmpty() {
      expectedException.expect(IllegalArgumentException.class);
      expectedException.expectMessage("url");

      SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(getClass())
            .withContact(VALID_NAME, VALID_EMAIL, "");
   }


   @Test
   public void shouldThrowNullPointerExceptionWhenLicenseNameIsNullOneArg() {
      expectedException.expect(NullPointerException.class);
      expectedException.expectMessage("name");

      SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(getClass())
            .withLicense(null);
   }

   @Test
   public void shouldThrowIllegalArgumentExceptionWhenLicenseNameIsEmptyOneArg() {
      expectedException.expect(IllegalArgumentException.class);
      expectedException.expectMessage("name");

      SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(getClass())
            .withLicense("");
   }

   @Test
   public void shouldThrowNullPointerExceptionWhenLicenseNameIsNullTwoArgs() {
      expectedException.expect(NullPointerException.class);
      expectedException.expectMessage("name");

      SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(getClass())
            .withLicense(null, VALID_URL);
   }

   @Test
   public void shouldThrowIllegalArgumentExceptionWhenLicenseNameIsEmptyTwoArgs() {
      expectedException.expect(IllegalArgumentException.class);
      expectedException.expectMessage("name");

      SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(getClass())
            .withLicense("", VALID_URL);
   }

   @Test
   public void shouldThrowNullPointerExceptionWhenLicenseUrlIsNullTwoArgs() {
      expectedException.expect(NullPointerException.class);
      expectedException.expectMessage("url");

      SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(getClass())
            .withLicense(VALID_NAME, null);
   }

   @Test
   public void shouldThrowIllegalArgumentExceptionWhenLicenseUrlIsEmptyTwoArgs() {
      expectedException.expect(IllegalArgumentException.class);
      expectedException.expectMessage("url");

      SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(getClass())
            .withLicense(VALID_NAME, "");
   }

   @Test
   public void shouldReturnBundleWithOneResourcePackage() {
      String aPackage = "com.google";

      SwaggerBundle bundle = SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackage(aPackage)
            .build();

      assertThat(bundle.getResourcePackages()).isEqualTo(
            join(",",
                  "org.sdase.commons.optional.server.swagger.parameter.embed",
                  "org.sdase.commons.optional.server.swagger.json.example",
                  "org.sdase.commons.optional.server.swagger.sort",
                  aPackage
            )
      );
   }

   @Test
   public void shouldReturnBundleWithoutEmbedParameterSupport() {
      String aPackage = "com.google";

      SwaggerBundle bundle = SwaggerBundle.builder()
          .withTitle(VALID_TITLE)
          .addResourcePackage(aPackage)
          .disableEmbedParameter()
          .build();

      assertThat(bundle.getResourcePackages()).doesNotContain("org.sdase.commons.optional.server.swagger.parameter.embed");
   }

   @Test
   public void shouldReturnBundleWithoutJsonExamplesSupport() {
      String aPackage = "com.google";

      SwaggerBundle bundle = SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackage(aPackage)
            .disableJsonExamples()
            .build();

      assertThat(bundle.getResourcePackages()).doesNotContain("org.sdase.commons.optional.server.swagger.json.example");
   }

   @Test
   public void shouldReturnBundleWithOneResourcePackageClass() {
      Class<?> resourcePackageClass = getClass();

      SwaggerBundle bundle = SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(resourcePackageClass)
            .build();

      assertThat(bundle.getResourcePackages()).isEqualTo(
            join(",",
                  "org.sdase.commons.optional.server.swagger.parameter.embed",
                  "org.sdase.commons.optional.server.swagger.json.example",
                  "org.sdase.commons.optional.server.swagger.sort",
                  resourcePackageClass.getPackage().getName()
            )
      );
   }

   @Test
   public void shouldReturnBundleWithSeveralResourcePackages() {
      Class<?> resourcePackageClass1 = getClass();
      Class<?> resourcePackageClass2 = InetAddress.class;

      String aPackage = "com.google";

      SwaggerBundle bundle = SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(resourcePackageClass1)
            .addResourcePackage(aPackage)
            .addResourcePackageClass(resourcePackageClass2)
            .build();

      assertThat(bundle.getResourcePackages()).isEqualTo(
            join(",",
                "org.sdase.commons.optional.server.swagger.parameter.embed",
                "org.sdase.commons.optional.server.swagger.json.example",
                "org.sdase.commons.optional.server.swagger.sort",
                  resourcePackageClass1.getPackage().getName(),
                  aPackage,
                  resourcePackageClass2.getPackage().getName()));
   }

   @Test
   public void shouldAllowInitialization() {
      assertThatCode(() -> {
         SwaggerBundle bundle = SwaggerBundle.builder()
               .withTitle(VALID_TITLE)
               .addResourcePackageClass(String.class)
               .build();

         bundle.initialize(bootstrap);
      }).doesNotThrowAnyException();
   }

   @Test
   public void shouldUseDefaultBasePathIfJerseyIsNotUsed() {
      when(environment.jersey()).thenReturn(jerseyEnvironment);
      when(servletEnvironment.addFilter(any(String.class), any(Class.class))).thenReturn(crossOriginFilter);
      when(environment.servlets()).thenReturn(servletEnvironment);
      when(configuration.getServerFactory()).thenReturn(mock(ServerFactory.class));

      assertBeanConfigBasePath("/api");
   }

   @Test
   public void shouldHandleJerseyRootPathNotSet() {
      when(environment.jersey()).thenReturn(jerseyEnvironment);
      when(servletEnvironment.addFilter(any(String.class), any(Class.class))).thenReturn(crossOriginFilter);
      when(environment.servlets()).thenReturn(servletEnvironment);
      when(configuration.getServerFactory()).thenReturn(serverFactory);
      when(serverFactory.getJerseyRootPath()).thenReturn(Optional.empty());

      assertBeanConfigBasePath("/");
   }

   @Test
   public void shouldHandleJerseyRootPathSetToRoot() {
      when(environment.jersey()).thenReturn(jerseyEnvironment);
      when(servletEnvironment.addFilter(any(String.class), any(Class.class))).thenReturn(crossOriginFilter);
      when(environment.servlets()).thenReturn(servletEnvironment);
      when(configuration.getServerFactory()).thenReturn(serverFactory);
      when(serverFactory.getJerseyRootPath()).thenReturn(Optional.of("/*"));

      assertBeanConfigBasePath("/");
   }

   @Test
   public void shouldUseJerseyRootPath() {
      when(environment.jersey()).thenReturn(jerseyEnvironment);
      when(servletEnvironment.addFilter(any(String.class), any(Class.class))).thenReturn(crossOriginFilter);
      when(environment.servlets()).thenReturn(servletEnvironment);
      when(configuration.getServerFactory()).thenReturn(serverFactory);
      when(serverFactory.getJerseyRootPath()).thenReturn(Optional.of("/root/path/*"));

      assertBeanConfigBasePath("/root/path");
   }

   @Test
   public void shouldBuildSwaggerDocumentation() {
      when(environment.jersey()).thenReturn(jerseyEnvironment);
      when(servletEnvironment.addFilter(any(String.class), any(Class.class))).thenReturn(crossOriginFilter);
      when(environment.servlets()).thenReturn(servletEnvironment);

      String title = "example-title";
      String version = "2.0";
      String description = "example-description";
      String termsOfServiceUrl = "https://terms.of/service";

      Class<?> resourcePackageClass = getClass();

      SwaggerBundle swaggerBundle = SwaggerBundle.builder()
            .withTitle(title)
            .addResourcePackageClass(resourcePackageClass)
            .withVersion(version)
            .withDescription(description)
            .withTermsOfServiceUrl(termsOfServiceUrl)
            .build();

      assertThat(swaggerBundle.getBeanConfig()).isNull();

      swaggerBundle.run(configuration, environment);

      BeanConfig beanConfig = swaggerBundle.getBeanConfig();

      assertThat(beanConfig.getScan()).isTrue();
      assertThat(beanConfig.getBasePath()).isEqualTo("/api");
      assertThat(beanConfig.getResourcePackage()).isEqualTo(
            join(",",
               "org.sdase.commons.optional.server.swagger.parameter.embed",
               "org.sdase.commons.optional.server.swagger.json.example",
               "org.sdase.commons.optional.server.swagger.sort",
               resourcePackageClass.getPackage().getName()
            )
      );

      Info info = beanConfig.getSwagger().getInfo();

      assertThat(info.getTitle()).isEqualTo(title);
      assertThat(info.getVersion()).isEqualTo(version);
      assertThat(info.getDescription()).isEqualTo(description);
      assertThat(info.getTermsOfService()).isEqualTo(termsOfServiceUrl);
   }

   private void assertBeanConfigBasePath(String s) {
      SwaggerBundle swaggerBundle = SwaggerBundle.builder()
            .withTitle(VALID_TITLE)
            .addResourcePackageClass(getClass())
            .build();

      assertThat(swaggerBundle.getBeanConfig()).isNull();

      swaggerBundle.run(configuration, environment);

      assertThat(swaggerBundle.getBeanConfig().getBasePath()).isEqualTo(s);
   }
}
