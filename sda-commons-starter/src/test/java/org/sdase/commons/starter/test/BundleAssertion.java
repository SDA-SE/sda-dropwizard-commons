package org.sdase.commons.starter.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sdase.commons.starter.SdaPlatformBundle;

/**
 * Helper class to assert that the {@link SdaPlatformBundle} configures other bundles as expected.
 *
 * <p>The {@code BundleAssertion} has to be initialized in a setup method annotated with {@link
 * org.junit.Before}.
 */
public class BundleAssertion<C extends Configuration> {

  private final Bootstrap<C> bootstrapMock;

  private ArgumentCaptor<ConfiguredBundle> configuredBundlesCaptor;

  private boolean verified;

  public BundleAssertion() {
    configuredBundlesCaptor = ArgumentCaptor.forClass(ConfiguredBundle.class);
    //noinspection unchecked
    bootstrapMock = Mockito.mock(Bootstrap.class);
  }

  public void assertBundleConfiguredByPlatformBundle(
      SdaPlatformBundle<C> actual, ConfiguredBundle<?> expectedBundle) {
    assertThat(getBundleOfType(actual, expectedBundle.getClass()))
        .usingRecursiveComparison()
        .isEqualTo(expectedBundle);
  }

  public void assertBundleNotConfiguredByPlatformBundle(
      SdaPlatformBundle<C> bundle, Class<?> notExpectedBundleClass) {
    assertThat(getBundleOfType(bundle, notExpectedBundleClass)).isNull();
  }

  public int countAddedBundles(SdaPlatformBundle<C> platformBundle) {
    gatherBundles(platformBundle);
    return configuredBundlesCaptor.getAllValues().size();
  }

  public <T> T getBundleOfType(SdaPlatformBundle<C> platformBundle, Class<T> bundleClass) {
    gatherBundles(platformBundle);
    //noinspection unchecked
    return (T)
        configuredBundlesCaptor.getAllValues().stream()
            .filter(configuredBundle -> bundleClass.isAssignableFrom(configuredBundle.getClass()))
            .findFirst()
            .orElse(null);
  }

  private synchronized void gatherBundles(SdaPlatformBundle<C> platformBundle) {
    if (verified) {
      return;
    }
    platformBundle.initialize(bootstrapMock);
    //noinspection unchecked
    Mockito.verify(bootstrapMock, atLeastOnce()).addBundle(configuredBundlesCaptor.capture());
    verified = true;
  }
}
