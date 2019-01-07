package org.sdase.commons.server.starter.test;

import io.dropwizard.Bundle;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sdase.commons.server.starter.SdaPlatformBundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;

/**
 * <p>
 *    Helper class to assert that the {@link org.sdase.commons.server.starter.SdaPlatformBundle} configures other bundles
 *    as expected.
 * </p>
 * <p>
 *     The {@code BundleAssertion} has to be initialized in a setup method annotated with {@link org.junit.Before}.
 * </p>
 */
public class BundleAssertion<C extends Configuration> {

   private Bootstrap<C> bootstrapMock;

   private ArgumentCaptor<Bundle> bundlesCaptor;
   private ArgumentCaptor<ConfiguredBundle> configuredBundlesCaptor;

   private boolean verified;

   public BundleAssertion() {
      bundlesCaptor = ArgumentCaptor.forClass(Bundle.class);
      configuredBundlesCaptor = ArgumentCaptor.forClass(ConfiguredBundle.class);
      //noinspection unchecked
      bootstrapMock = Mockito.mock(Bootstrap.class);
   }

   public void assertBundleConfiguredByPlatformBundle(SdaPlatformBundle<C> actual, Bundle expectedBundle) {
      assertThat(getBundleOfType(actual, expectedBundle.getClass()))
            .isEqualToComparingFieldByFieldRecursively(expectedBundle);
   }

   public void assertBundleConfiguredByPlatformBundle(SdaPlatformBundle<C> actual, ConfiguredBundle<?> expectedBundle) {
      assertThat(getBundleOfType(actual, expectedBundle.getClass()))
            .isEqualToComparingFieldByFieldRecursively(expectedBundle);
   }

   public void assertBundleNotConfiguredByPlatformBundle(SdaPlatformBundle<C> bundle, Class<?> notExpectedBundleClass) {
      assertThat(getBundleOfType(bundle, notExpectedBundleClass))
            .isNull();
   }

   public int countAddedBundles(SdaPlatformBundle<C> platformBundle) {
      gatherBundles(platformBundle);
      return bundlesCaptor.getAllValues().size() + configuredBundlesCaptor.getAllValues().size();
   }

   public  <T> T getBundleOfType(SdaPlatformBundle<C> platformBundle, Class<T> bundleClass) {
      gatherBundles(platformBundle);
      //noinspection unchecked
      return bundlesCaptor.getAllValues().stream()
            .filter(b -> bundleClass.isAssignableFrom(b.getClass()))
            .findFirst()
            .map(bundle -> (T) bundle)
            .orElseGet(() -> (T) configuredBundlesCaptor.getAllValues().stream()
                  .filter(configuredBundle -> bundleClass.isAssignableFrom(configuredBundle.getClass()))
                  .findFirst().orElse(null)
            );
   }

   private synchronized void gatherBundles(SdaPlatformBundle<C> platformBundle) {
      if (verified) {
         return;
      }
      platformBundle.initialize(bootstrapMock);
      Mockito.verify(bootstrapMock, atLeastOnce()).addBundle(bundlesCaptor.capture());
      //noinspection unchecked
      Mockito.verify(bootstrapMock, atLeastOnce()).addBundle(configuredBundlesCaptor.capture());
      verified = true;
   }
}
