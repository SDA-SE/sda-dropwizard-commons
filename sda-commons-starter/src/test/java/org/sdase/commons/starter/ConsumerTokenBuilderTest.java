package org.sdase.commons.starter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sdase.commons.server.consumer.ConsumerTokenBundle;
import org.sdase.commons.starter.test.BundleAssertion;

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
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withoutConsumerTokenSupport()
            .addOpenApiResourcePackageClass(this.getClass())
            .build();

    bundleAssertion.assertBundleNotConfiguredByPlatformBundle(bundle, ConsumerTokenBundle.class);
  }

  @Test
  public void withOptionalConsumerToken() {
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withOptionalConsumerToken()
            .addOpenApiResourcePackageClass(this.getClass())
            .build();

    verifyRegisteredConsumerTokenFiltersEqual(
        bundle, ConsumerTokenBundle.builder().withOptionalConsumerToken().build());
  }

  @Test
  public void withRequiredConsumerToken() {
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withRequiredConsumerToken()
            .addOpenApiResourcePackageClass(this.getClass())
            .build();

    verifyRegisteredConsumerTokenFiltersEqual(
        bundle, ConsumerTokenBundle.builder().withRequiredConsumerToken().build());
  }

  @Test
  public void withRequiredConsumerTokenAndExcludedPaths() {
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withRequiredConsumerToken()
            .withExcludePatternsForRequiredConsumerToken("/public", "/ping")
            .addOpenApiResourcePackageClass(this.getClass())
            .build();

    verifyRegisteredConsumerTokenFiltersEqual(
        bundle,
        ConsumerTokenBundle.builder()
            .withRequiredConsumerToken()
            .withExcludePatterns("/public", "/ping")
            .build());
  }

  @Test
  public void withConfigurableConsumerToken() {
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withConsumerTokenConfigProvider(SdaPlatformConfiguration::getConsumerToken)
            .addOpenApiResourcePackageClass(this.getClass())
            .build();

    verifyRegisteredConsumerTokenFiltersEqual(
        bundle,
        ConsumerTokenBundle.builder()
            .withConfigProvider(SdaPlatformConfiguration::getConsumerToken)
            .build(),
        SdaPlatformConfiguration.class);
  }

  private void verifyRegisteredConsumerTokenFiltersEqual(
      SdaPlatformBundle<SdaPlatformConfiguration> actualSdaPlatformBundle,
      ConsumerTokenBundle<Configuration> expectedBundle) {
    verifyRegisteredConsumerTokenFiltersEqual(
        actualSdaPlatformBundle, expectedBundle, Configuration.class);
  }

  private <C extends Configuration> void verifyRegisteredConsumerTokenFiltersEqual(
      SdaPlatformBundle<SdaPlatformConfiguration> actualSdaPlatformBundle,
      ConsumerTokenBundle<C> expectedBundle,
      Class<C> configurationClass) {
    try {
      //noinspection unchecked
      ConsumerTokenBundle<SdaPlatformConfiguration> actualConsumerTokenBundle =
          bundleAssertion.getBundleOfType(actualSdaPlatformBundle, ConsumerTokenBundle.class);

      actualConsumerTokenBundle.run(new SdaPlatformConfiguration(), environmentMock);
      expectedBundle.run(configurationClass.newInstance(), environmentMock);

      verify(environmentMock.jersey(), times(2)).register(jerseyRegistrationCaptor.capture());

      List<Object> registeredFilters = jerseyRegistrationCaptor.getAllValues();
      assertThat(registeredFilters.get(0))
          .isEqualToComparingFieldByFieldRecursively(registeredFilters.get(1));
    } catch (InstantiationException | IllegalAccessException e) {
      fail("Fail to instantiate config class.", e);
    }
  }
}
