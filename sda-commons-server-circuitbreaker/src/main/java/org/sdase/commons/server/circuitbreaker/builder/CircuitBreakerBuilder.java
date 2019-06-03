package org.sdase.commons.server.circuitbreaker.builder;

import io.dropwizard.Configuration;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.sdase.commons.server.circuitbreaker.CircuitBreakerConfiguration;
import org.sdase.commons.server.circuitbreaker.CircuitBreakerConfigurationProvider;
import org.sdase.commons.server.circuitbreaker.CircuitBreakerWrapperHelper;

public class CircuitBreakerBuilder<T extends Configuration> implements CircuitBreakerConfigurationBuilder<T>,
      CircuitBreakerExceptionBuilder<T>, CircuitBreakerFinalBuilder {

   private final String name;
   private final CircuitBreakerRegistry registry;
   private final T configuration;
   private CircuitBreakerConfigurationProvider<T> configurationProvider;
   private Class<? extends Throwable>[] recordedErrorClasses;
   private Class<? extends Throwable>[] ignoredErrorClasses;

   public CircuitBreakerBuilder(String name, CircuitBreakerRegistry registry, T configuration) {
      this.name = name;
      this.registry = registry;
      this.configuration = configuration;
   }

   @Override
   public CircuitBreakerExceptionBuilder<T> withCustomConfig(CircuitBreakerConfiguration config) {
      this.configurationProvider = c -> config;
      return this;
   }

   @Override
   public CircuitBreakerExceptionBuilder<T> withConfigProvider(CircuitBreakerConfigurationProvider<T> provider) {
      this.configurationProvider = provider;
      return this;
   }

   @Override
   public CircuitBreakerExceptionBuilder<T> withDefaultConfig() {
      this.configurationProvider = null;
      return this;
   }

   @Override
   public CircuitBreakerExceptionBuilder<T> recordExceptions(Class<? extends Throwable>... errorClasses) {
      this.recordedErrorClasses = errorClasses;
      return this;
   }

   @Override
   public CircuitBreakerExceptionBuilder<T> ignoreExceptions(Class<? extends Throwable>... errorClasses) {
      this.ignoredErrorClasses = errorClasses;
      return this;
   }

   @Override
   public CircuitBreaker build() {
      CircuitBreakerConfig config = createCircuitBreakerConfig();
      return registry.circuitBreaker(name, config);
   }

   @Override
   public <U> U wrap(U target) {
      CircuitBreaker circuitBreaker = build();
      return CircuitBreakerWrapperHelper.wrapWithCircuitBreaker(target, circuitBreaker);
   }

   private CircuitBreakerConfig createCircuitBreakerConfig() {
      if (configurationProvider != null) {
         CircuitBreakerConfiguration circuitBreakerConfiguration = configurationProvider.apply(configuration);
         return CircuitBreakerConfig
               .custom()
               .enableAutomaticTransitionFromOpenToHalfOpen()
               .failureRateThreshold(circuitBreakerConfiguration.getFailureRateThreshold())
               .ringBufferSizeInClosedState(circuitBreakerConfiguration.getRingBufferSizeInClosedState())
               .ringBufferSizeInHalfOpenState(circuitBreakerConfiguration.getRingBufferSizeInHalfOpenState())
               .waitDurationInOpenState(circuitBreakerConfiguration.getWaitDurationInOpenState())
               .recordExceptions(recordedErrorClasses)
               .ignoreExceptions(ignoredErrorClasses)
               .build();
      }
      return registry.getDefaultConfig();
   }
}
