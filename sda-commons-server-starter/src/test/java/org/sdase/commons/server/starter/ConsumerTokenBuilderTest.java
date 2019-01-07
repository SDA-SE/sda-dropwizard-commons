package org.sdase.commons.server.starter;

import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sdase.commons.server.consumer.ConsumerTokenBundle;
import org.sdase.commons.server.starter.test.BundleAssertion;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ConsumerTokenBuilderTest {

   private BundleAssertion<SdaPlatformConfiguration> bundleAssertion;

   private Environment environmentMock;
   private ArgumentCaptor<Object> jerseyRegistrationCaptor;

   @Before
   public void setUp() {
      bundleAssertion = new BundleAssertion<>();
      environmentMock = mock(Environment.class, RETURNS_DEEP_STUBS);
      jerseyRegistrationCaptor = ArgumentCaptor.forClass(Object.class);
   }

   @Test
   public void withoutConsumerToken() {
      SdaPlatformBundle<SdaPlatformConfiguration> bundle = SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withoutConsumerTokenSupport()
            .withSwaggerInfoTitle("Starter") // NOSONAR
            .addSwaggerResourcePackageClass(this.getClass())
            .build();

      bundleAssertion.assertBundleNotConfiguredByPlatformBundle(bundle, ConsumerTokenBundle.class);
   }

   @Test
   public void withOptionalConsumerToken() {
      SdaPlatformBundle<SdaPlatformConfiguration> bundle = SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withOptionalConsumerToken()
            .withSwaggerInfoTitle("Starter")
            .addSwaggerResourcePackageClass(this.getClass())
            .build();

      verifyRegisteredConsumerTokenFiltersEqual(
            bundle,
            ConsumerTokenBundle.builder().withOptionalConsumerToken().build()
      );
   }

   @Test
   public void withRequiredConsumerToken() {
      SdaPlatformBundle<SdaPlatformConfiguration> bundle = SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withRequiredConsumerToken()
            .withSwaggerInfoTitle("Starter")
            .addSwaggerResourcePackageClass(this.getClass())
            .build();

      verifyRegisteredConsumerTokenFiltersEqual(
            bundle,
            ConsumerTokenBundle.builder().withRequiredConsumerToken().build()
      );
   }

   @Test
   public void withRequiredConsumerTokenAndExcludedPaths() {
      SdaPlatformBundle<SdaPlatformConfiguration> bundle = SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withRequiredConsumerToken()
            .withExcludePatternsForRequiredConsumerToken("/public", "/ping")
            .withSwaggerInfoTitle("Starter")
            .addSwaggerResourcePackageClass(this.getClass())
            .build();

      verifyRegisteredConsumerTokenFiltersEqual(bundle, ConsumerTokenBundle.builder()
            .withRequiredConsumerToken()
            .withExcludePatterns("/public", "/ping")
            .build());
   }

   private void verifyRegisteredConsumerTokenFiltersEqual(
         SdaPlatformBundle<SdaPlatformConfiguration> actualSdaPlatformBundle,
         ConsumerTokenBundle<Configuration> expectedBundle) {

      //noinspection unchecked
      ConsumerTokenBundle<SdaPlatformConfiguration> actualConsumerTokenBundle = bundleAssertion.getBundleOfType(
            actualSdaPlatformBundle, ConsumerTokenBundle.class);

      actualConsumerTokenBundle.run(mock(SdaPlatformConfiguration.class), environmentMock);
      expectedBundle.run(mock(Configuration.class), environmentMock);

      verify(environmentMock.jersey(), times(2)).register(jerseyRegistrationCaptor.capture());

      List<Object> registeredFilters = jerseyRegistrationCaptor.getAllValues();
      assertThat(registeredFilters.get(0)).isEqualToComparingFieldByFieldRecursively(registeredFilters.get(1));
   }
}
