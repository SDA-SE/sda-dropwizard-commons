package org.sdase.commons.server.dropwizard.bundles;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * <p>A bundle that creates a {@link Supplier} for a configuration value.</p>
 * <p>
 *    Having a {@link #supplier()} knowing only the return type may be needed if the consumer should not know or can
 *    not know about the configuration class. Using a supplier without referencing the configuration class also makes
 *    it easier to replace a configured value with a value supplied by a service.
 * </p>
 * <p>
 *    In most cases when bundles are initialized it is easier to use a {@code Function<C extends Configuration, R>}
 *    directly created from the configuration class as method reference: {@code MyConfigClass::getValue}
 * </p>
 *
 *
 * @param <C> the configuration type
 * @param <R> the type of the supplied value
 */
public class ConfigurationValueSupplierBundle<C extends Configuration, R> implements ConfiguredBundle<C> {

   private C configuration;
   private Function<C, R> configurationAccessor;
   private boolean initialized;

   private ConfigurationValueSupplierBundle(Function<C, R> configurationAccessor) {
      this.configurationAccessor = configurationAccessor;
   }

   public static InitialBuilder builder() {
      return new Builder<>();
   }

   /**
    * @return a supplier providing the value of the configuration wrapped in an {@link Optional}. The supplier will
    *         fail with an {@link IllegalStateException} until {@link ConfiguredBundle#run(Object, Environment)} has
    *         been executed.
    */
   public Supplier<Optional<R>> supplier() {
      return () -> {
         if (!initialized) {
            throw new IllegalStateException("Could not access configuration before bundle passed run(C, Environment)");
         }
         return Optional.ofNullable(configurationAccessor.apply(configuration));
      };
   }

   /**
    * @return a supplier providing the value of the configuration. The supplier will fail with an
    *         {@link IllegalStateException} until {@link ConfiguredBundle#run(Object, Environment)} has been executed.
    *         The supplier may return null.
    */
   public Supplier<R> valueSupplier() {
      return () -> supplier().get().orElse(null);
   }

   @Override
   public void initialize(Bootstrap<?> bootstrap) {
      // nothing to initialize
   }

   @Override
   public void run(C configuration, Environment environment) {
      this.configuration = configuration;
      initialized = true;
   }

   //
   // Builder
   //

   public interface InitialBuilder {
      /**
       * @param configurationAccessor A method reference to access the current configuration, e.g.
       *                              {@code MyConfiguration::getValue}
       * @param <C> the configuration class
       * @param <R> the accessed return type
       * @return a builder to complete the configuration
       */
      <C extends Configuration, R> FinalBuilder<C, R> withAccessor(Function<C, R> configurationAccessor);
   }

   public interface FinalBuilder<C extends Configuration, R> {
      ConfigurationValueSupplierBundle<C, R> build();
   }

   public static class Builder<C extends Configuration, R> implements InitialBuilder, FinalBuilder<C, R> {

      private Function<C, R> configurationAccessor;

      private Builder() {
      }

      private Builder(Function<C, R> configurationAccessor) {
         this.configurationAccessor = configurationAccessor;
      }

      @Override
      public <C1 extends Configuration, R1> FinalBuilder<C1, R1> withAccessor(Function<C1, R1> configurationAccessor) {
         return new Builder<>(configurationAccessor);
      }

      @Override
      public ConfigurationValueSupplierBundle<C, R> build() {
         return new ConfigurationValueSupplierBundle<>(configurationAccessor);
      }
   }

}
