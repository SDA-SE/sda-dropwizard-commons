package com.sdase.commons.server.consumer;

import com.sdase.commons.server.consumer.filter.ConsumerTokenServerFilter;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.function.Function;

public class ConsumerTokenBundle<C extends Configuration> implements ConfiguredBundle<C> {

   private ConsumerTokenConfigProvider<C> consumerTokenConfigProvider;

   private ConsumerTokenBundle(ConsumerTokenConfigProvider<C> consumerTokenConfigProvider) {
      this.consumerTokenConfigProvider = consumerTokenConfigProvider;
   }

   @Override
   public void initialize(Bootstrap<?> bootstrap) {
      // nothing to initialize
   }

   @Override
   public void run(C configuration, Environment environment) {
      ConsumerTokenConfig config = consumerTokenConfigProvider.apply(configuration);
      boolean requireIdentifiedConsumer = !config.isOptional();
      ConsumerTokenServerFilter consumerTokenServerFilter = new ConsumerTokenServerFilter(requireIdentifiedConsumer);
      environment.jersey().register(consumerTokenServerFilter);
   }

   public static ConfigBuilder builder() {
      return new Builder<>();
   }

   @FunctionalInterface
   public interface ConsumerTokenConfigProvider<C extends Configuration> extends Function<C, ConsumerTokenConfig> {}


   //
   // builder
   //

   public interface ConfigBuilder {
      /**
       *
       * @param configProvider the method returning the {@link ConsumerTokenConfig} from the applications
       *                       {@link Configuration}.
       * @param <C> the applications configuration type
       * @return a builder to create the bundle
       */
      <C extends Configuration> FinalBuilder<C> withConfigProvider(ConsumerTokenConfigProvider<C> configProvider);

      /**
       * Creates the bundle with a consumer token required for every request.
       *
       * @return a builder to create the bundle
       */
      FinalBuilder<Configuration> withRequiredConsumerToken();

      /**
       * Creates the bundle with the consumer token being optional.
       *
       * @return a builder to create the bundle
       */
      FinalBuilder<Configuration> withOptionalConsumerToken();
   }

   public interface FinalBuilder<C extends Configuration> {
      ConsumerTokenBundle<C> build();
   }

   public static class Builder<C extends Configuration> implements ConfigBuilder, FinalBuilder<C> {

      private ConsumerTokenConfigProvider<C> configProvider;

      private Boolean requireConsumerToken;

      private Builder() {
      }

      private Builder(boolean requireConsumerToken) {
         this.requireConsumerToken = requireConsumerToken;
      }

      private Builder(ConsumerTokenConfigProvider<C> configProvider) {
         this.configProvider = configProvider;
      }

      @Override
      public <T extends Configuration> FinalBuilder<T> withConfigProvider(ConsumerTokenConfigProvider<T> configProvider) {
         return new Builder<>(configProvider);
      }

      @Override
      public FinalBuilder<Configuration> withRequiredConsumerToken() {
         return new Builder<>(true);
      }

      @Override
      public FinalBuilder<Configuration> withOptionalConsumerToken() {
         return new Builder<>(false);
      }

      @Override
      public ConsumerTokenBundle<C> build() {
         if (configProvider != null) {
            return new ConsumerTokenBundle<>(configProvider);
         }
         else if (requireConsumerToken != null){
            ConsumerTokenConfig consumerTokenConfig = new ConsumerTokenConfig();
            consumerTokenConfig.setOptional(!requireConsumerToken);
            return new ConsumerTokenBundle<>(c -> consumerTokenConfig);
         }
         else throw new IllegalStateException("Missing either a config provider or an explicit config for required" +
                  " consumer token.");
      }
   }
}
