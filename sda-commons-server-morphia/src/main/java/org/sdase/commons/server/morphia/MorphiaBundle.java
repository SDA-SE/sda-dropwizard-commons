package org.sdase.commons.server.morphia;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.client.MongoDatabase;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.apache.commons.lang3.Validate;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.converters.LocalDateConverter;
import org.mongodb.morphia.converters.LocalDateTimeConverter;
import org.sdase.commons.server.morphia.converter.ZonedDateTimeConverter;

import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class MorphiaBundle<C extends Configuration> implements ConfiguredBundle<C> {

   private final Function<C, MongoConfiguration> configurationProvider;
   private final Consumer<MongoClientOptions.Builder> clientOptions = c -> {}; //NOSONAR

   private final Set<String> packagesToScan;

   private MongoConfiguration mongoConfiguration;

   private Environment environment;

   // created database after initialization
   private MongoDatabase database;
   private Datastore morphiaDatastore;

   private MorphiaBundle(MongoConfigurationProvider<C> configProvider, Consumer<MongoClientOptions.Builder> cob, Set<String> packagesToScan) {
      this.configurationProvider = configProvider;
      this.clientOptions.andThen(cob);
      this.packagesToScan = packagesToScan;
   }

   @Override
   public void initialize(Bootstrap<?> bootstrap) {
      // nothing to do here
   }

   @Override
   public void run(C configuration, Environment environment) {
      this.environment = environment;
      this.mongoConfiguration = configurationProvider.apply(configuration);

      this.database = createClient().getDatabase(mongoConfiguration.getDatabase());

      Morphia configuredMorphia = new Morphia();
      configuredMorphia.getMapper().getConverters().addConverter(new LocalDateTimeConverter());
      configuredMorphia.getMapper().getConverters().addConverter(new LocalDateConverter());
      configuredMorphia.getMapper().getConverters().addConverter(new ZonedDateTimeConverter());
      this.packagesToScan.forEach(configuredMorphia::mapPackage);
      this.morphiaDatastore = configuredMorphia.createDatastore(createClient(), mongoConfiguration.getDatabase());
      this.morphiaDatastore.ensureIndexes();

   }

   private MongoClient createClient() {
      return new MongoClientBuilder()
            .withConfiguration(mongoConfiguration)
            .addMongoClientOption(clientOptions)
            .build(environment);
   }

   public MongoClient getClient() {
      return createClient();
   }

   public MongoDatabase getDatabase() {
      return database;
   }

   public MongoConfiguration getMongoConfiguration() {
      return mongoConfiguration;
   }

   public Datastore getMorphiaDatastore() {
     return morphiaDatastore;
   }

   //
   // Builder
   //
   public static InitialBuilder builder() {
      return new Builder();
   }

   public interface InitialBuilder {
      /**
       * @param configurationProvider
       *           the method reference that provides
       *           the @{@link MongoConfiguration} from the applications
       *           configurations class
       */
      <C extends Configuration> ScanPackageBuilder<C> withConfigurationProvider(
            @NotNull MongoConfigurationProvider<C> configurationProvider);
   }

   public interface ScanPackageBuilder<C extends Configuration> {
      /**
       * @param packageToScanForEntities The package that should be scanned for entities recursively.
       */
      FinalBuilder<C> withEntityScanPackage(@NotNull String packageToScanForEntities);

      /**
       * @param markerClass A class or interface that defines the base package for recursive entity scanning. The class
       *                    may be a marker interface or a specific entity class.
       */
      FinalBuilder<C> withEntityScanPackageClass(@NotNull Class<?> markerClass);
   }

   public interface FinalBuilder<C> {
      /**
       * Adds a client option to the builder with that the client is initialized
       * @param cob client option builder consumer
       * @return final builder
       */
      FinalBuilder<C> withClientOption(Consumer<MongoClientOptions.Builder> cob);

      /**
       * Builds the mongo bundle
       * @return mongo bundle
       */
      MorphiaBundle build();
   }

   public static class Builder<T extends Configuration> implements InitialBuilder, ScanPackageBuilder<T>, FinalBuilder<T> {

      private MongoConfigurationProvider<T> configProvider;
      private Consumer<MongoClientOptions.Builder> cob = c -> {
      };
      private Set<String> packagesToScan = new HashSet<>();

      private Builder() {
      }

      private Builder(MongoConfigurationProvider<T> configProvider) {
         this.configProvider = configProvider;
      }

      @Override
      public <C extends Configuration> ScanPackageBuilder<C> withConfigurationProvider(
            MongoConfigurationProvider<C> configurationProvider) {
         return new Builder<>(configurationProvider);
      }

      @Override
      public FinalBuilder<T> withClientOption(Consumer<MongoClientOptions.Builder> cob) {
         this.cob.andThen(cob);
         return this;
      }

      @Override
      public FinalBuilder<T> withEntityScanPackageClass(Class markerClass) {
         return withEntityScanPackage(markerClass.getPackage().getName());
      }

      @Override
      public FinalBuilder<T> withEntityScanPackage(String packageToScanForEntities) {
         packagesToScan.add(Validate.notBlank(packageToScanForEntities));
         return this;
      }

      @Override
      public MorphiaBundle build() {
         return new MorphiaBundle<>(configProvider, cob, packagesToScan);
      }
   }
}
