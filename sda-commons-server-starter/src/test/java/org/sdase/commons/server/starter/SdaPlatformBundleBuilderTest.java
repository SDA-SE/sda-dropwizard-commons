package org.sdase.commons.server.starter;

import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.prometheus.client.CollectorRegistry;
import org.junit.Before;
import org.junit.Test;
import org.sdase.commons.server.auth.config.AuthConfig;
import org.sdase.commons.server.jackson.JacksonConfigurationBundle;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SdaPlatformBundleBuilderTest {

   private Bootstrap bootstrapMock;
   private Environment environmentMock;

   @Before
   public void setUpMocks() {
      bootstrapMock = mock(Bootstrap.class, RETURNS_DEEP_STUBS);
      environmentMock = mock(Environment.class, RETURNS_DEEP_STUBS);
      DropwizardResourceConfig config = new DropwizardResourceConfig();
      config.register(JacksonConfigurationBundle.builder().build());
      when(environmentMock.jersey().getResourceConfig()).thenReturn(config);
      CollectorRegistry.defaultRegistry.clear();
   }

   @Test
   public void buildDefaultPlatformBundleWithoutException() throws Exception { // NOSONAR
      SdaPlatformBundle<SdaPlatformConfiguration> bundle = SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withRequiredConsumerToken()
            .withSwaggerInfoTitle("Only a Test")
            .addSwaggerResourcePackageClass(this.getClass())
            .build();
      bundle.initialize(bootstrapMock);
      bundle.run(new SdaPlatformConfiguration(), environmentMock);
   }

   @Test
   public void buildCustomPlatformBundleWithoutException() throws Exception { // NOSONAR
      SdaPlatformBundle<SdaPlatformConfiguration> bundle = SdaPlatformBundle.builder()
            .usingCustomConfig(SdaPlatformConfiguration.class)
            .withAuthConfigProvider(c -> new AuthConfig())
            .withoutCorsSupport()
            .withoutConsumerTokenSupport()
            .withSwaggerInfoTitle("The test title")
            .addSwaggerResourcePackageClass(this.getClass())
            .build();
      bundle.initialize(bootstrapMock);
      bundle.run(new SdaPlatformConfiguration(), environmentMock);
   }

   @Test(expected = IllegalStateException.class)
   public void noCorsConfigurationIfCorsDisabled() {
      SdaPlatformBundle.builder()
            .usingCustomConfig(SdaPlatformConfiguration.class)
            .withAuthConfigProvider(c -> new AuthConfig())
            .withoutCorsSupport()
            .withoutConsumerTokenSupport()
            .withSwaggerInfoTitle("The test title")
            .addSwaggerResourcePackageClass(this.getClass())
            .withCorsAdditionalExposedHeaders("x-foo");
   }
}
