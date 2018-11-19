package org.sdase.commons.server.dropwizard.bundles;

import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.junit.Test;

import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

public class ConfigurationValueSupplierBundleTest {


   @Test(expected = IllegalStateException.class)
   public void failIfGetCalledBeforeInitialize() {
      ConfigurationValueSupplierBundle<TestConfig, String> bundle = ConfigurationValueSupplierBundle.builder()
            .withAccessor(TestConfig::getMyConfig)
            .build();

      Supplier<Optional<String>> myConfigSupplier = bundle.supplier();

      myConfigSupplier.get();
   }

   @Test(expected = IllegalStateException.class)
   public void failIfGetCalledBeforeRun() {
      ConfigurationValueSupplierBundle<TestConfig, String> bundle = ConfigurationValueSupplierBundle.builder()
            .withAccessor(TestConfig::getMyConfig)
            .build();

      bundle.initialize(mock(Bootstrap.class, RETURNS_DEEP_STUBS));

      Supplier<Optional<String>> myConfigSupplier = bundle.supplier();

      myConfigSupplier.get();
   }

   @Test
   public void notPresentOptionalForNull() {
      ConfigurationValueSupplierBundle<TestConfig, String> bundle = ConfigurationValueSupplierBundle.builder()
            .withAccessor(TestConfig::getMyConfig)
            .build();

      Supplier<Optional<String>> myConfigSupplier = bundle.supplier();

      bundle.initialize(mock(Bootstrap.class, RETURNS_DEEP_STUBS));
      bundle.run(new TestConfig(), mock(Environment.class, RETURNS_DEEP_STUBS));

      assertThat(myConfigSupplier.get()).isNotPresent();
   }

   @Test
   public void noExceptionIfValueIsNull() {
      ConfigurationValueSupplierBundle<TestConfig, String> bundle = ConfigurationValueSupplierBundle.builder()
            .withAccessor(TestConfig::getMyConfig)
            .build();

      Supplier<String> myConfigSupplier = bundle.valueSupplier();

      bundle.initialize(mock(Bootstrap.class, RETURNS_DEEP_STUBS));
      bundle.run(new TestConfig(), mock(Environment.class, RETURNS_DEEP_STUBS));

      assertThat(myConfigSupplier.get()).isNull();
   }

   @Test
   public void returnValue() {
      ConfigurationValueSupplierBundle<TestConfig, String> bundle = ConfigurationValueSupplierBundle.builder()
            .withAccessor(TestConfig::getMyConfig)
            .build();

      Supplier<String> myConfigSupplier = bundle.valueSupplier();

      bundle.initialize(mock(Bootstrap.class, RETURNS_DEEP_STUBS));
      bundle.run(new TestConfig().setMyConfig("test-config"), mock(Environment.class, RETURNS_DEEP_STUBS));

      assertThat(myConfigSupplier.get()).isEqualTo("test-config");
   }

   @Test
   public void returnValueInOptional() {
      ConfigurationValueSupplierBundle<TestConfig, String> bundle = ConfigurationValueSupplierBundle.builder()
            .withAccessor(TestConfig::getMyConfig)
            .build();

      Supplier<Optional<String>> myConfigSupplier = bundle.supplier();

      bundle.initialize(mock(Bootstrap.class, RETURNS_DEEP_STUBS));
      bundle.run(new TestConfig().setMyConfig("test-config"), mock(Environment.class, RETURNS_DEEP_STUBS));

      assertThat(myConfigSupplier.get()).isPresent().hasValue("test-config");
   }


   @SuppressWarnings("WeakerAccess")
   private static class TestConfig extends Configuration {
      private String myConfig;

      public String getMyConfig() {
         return myConfig;
      }

      public TestConfig setMyConfig(String myConfig) {
         this.myConfig = myConfig;
         return this;
      }
   }
}