package org.sdase.commons.server.starter.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sdase.commons.server.starter.SdaPlatformBundle;

/**
 * Helper class to assert that the {@link SdaPlatformBundle} configures other bundles as expected.
 *
 * <p>The {@code BundleAssertion} has to be initialized in a setup method annotated with {@link
 * org.junit.Before}.
 */
public class BundleAssertion<C extends Configuration> {

  private final Bootstrap<C> bootstrapMock;

  private final ArgumentCaptor<ConfiguredBundle> configuredBundlesCaptor;

  private final ArgumentCaptor<ObjectMapper> objectMapperCaptor;

  private boolean verified;

  public BundleAssertion() {
    configuredBundlesCaptor = ArgumentCaptor.forClass(ConfiguredBundle.class);
    objectMapperCaptor = ArgumentCaptor.forClass(ObjectMapper.class);
    //noinspection unchecked
    bootstrapMock = Mockito.mock(Bootstrap.class, RETURNS_DEEP_STUBS);
  }

  public void assertBundleConfiguredByPlatformBundle(
      SdaPlatformBundle<C> actual, ConfiguredBundle<?> expectedBundle) {
    assertThat(getBundleOfType(actual, expectedBundle.getClass()))
        // Please note that there are limitations to `usingRecursiveComparison` since AssertJ 3.23.0
        // For compatibility with Java 17 they use Object's equals method to compare instances
        // which might not work in all cases, e.g. if you use lambda functions as fields
        .usingRecursiveComparison()
        .ignoringFields("bootstrap")
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

  public ObjectMapper getObjectMapper(SdaPlatformBundle<C> platformBundle) {
    gatherBundles(platformBundle);
    return objectMapperCaptor.getValue();
  }

  private synchronized void gatherBundles(SdaPlatformBundle<C> platformBundle) {
    if (verified) {
      return;
    }
    platformBundle.initialize(bootstrapMock);
    //noinspection unchecked
    Mockito.verify(bootstrapMock, atLeastOnce()).addBundle(configuredBundlesCaptor.capture());

    configuredBundlesCaptor
        .getAllValues()
        .forEach(configuredBundle -> configuredBundle.initialize(bootstrapMock));

    Mockito.verify(bootstrapMock, atLeastOnce()).setObjectMapper(objectMapperCaptor.capture());
    verified = true;
  }
}
