package org.sdase.commons.server.morphia;

import com.mongodb.MongoClient;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.apache.commons.lang3.Validate;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.converters.LocalDateConverter;
import org.mongodb.morphia.converters.LocalDateTimeConverter;
import org.mongodb.morphia.converters.TypeConverter;
import org.sdase.commons.server.morphia.converter.ZonedDateTimeConverter;
import org.sdase.commons.server.morphia.internal.MongoClientBuilder;

import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

/**
 * A bundle used to create a connection to a MongoDB that is accessed through a
 * <a href="http://morphiaorg.github.io/morphia/">Morphia</a> {@link Datastore}.
 *
 * @param <C> The subtype of {@link Configuration}, the {@link io.dropwizard.Application} uses.
 */
public class MorphiaBundle<C extends Configuration> implements ConfiguredBundle<C> {

   private static final Set<TypeConverter> DEFAULT_CONVERTERS = new LinkedHashSet<>(asList(
         new LocalDateTimeConverter(),
         new LocalDateConverter(),
         new ZonedDateTimeConverter())
   );

   private final Function<C, MongoConfiguration> configurationProvider;

   private final Set<TypeConverter> customConverters = new LinkedHashSet<>();
   private final Set<String> packagesToScan = new HashSet<>();
   private final Set<Class> entityClasses = new HashSet<>();

   private MongoClient mongoClient;
   private Datastore morphiaDatastore;

   private MorphiaBundle(
         MongoConfigurationProvider<C> configProvider,
         Set<String> packagesToScan,
         Set<Class> entityClasses,
         Set<TypeConverter> customConverters) {
      this.configurationProvider = configProvider;
      this.packagesToScan.addAll(packagesToScan);
      this.entityClasses.addAll(entityClasses);
      this.customConverters.addAll(customConverters);
   }

   @Override
   public void initialize(Bootstrap<?> bootstrap) {
      // nothing to do here
   }

   @Override
   public void run(C configuration, Environment environment) {
      MongoConfiguration mongoConfiguration = configurationProvider.apply(configuration);

      Morphia configuredMorphia = new Morphia();
      customConverters.forEach(cc -> configuredMorphia.getMapper().getConverters().addConverter(cc));
      if (!entityClasses.isEmpty()) {
         configuredMorphia.map(entityClasses);
      }
      this.packagesToScan.forEach(configuredMorphia::mapPackage);
      this.mongoClient = createClient(environment, mongoConfiguration);
      this.morphiaDatastore = configuredMorphia.createDatastore(mongoClient, mongoConfiguration.getDatabase());
      this.morphiaDatastore.ensureIndexes();

   }

   /**
    * @return the {@link MongoClient} that is connected to the MongoDB cluster. The client may be used for raw MongoDB
    *         operations. Usually the Morphia {@linkplain #getDatastore() Datastore} should be preferred for database
    *         operations.
    * @throws IllegalStateException if the method is called before the mongoClient is initialized in
    *                               {@link #run(Configuration, Environment)}
    */
   public MongoClient getMongoClient() {
      if (mongoClient == null) {
         throw new IllegalStateException("Could not access mongoClient before Application#run(Configuration, Environment).");
      }
      return mongoClient;
   }

   /**
    * @return the configured {@link Datastore} that is ready to access the MongoDB defined in
    *         {@link MongoConfiguration#database}.
    * @throws IllegalStateException if the method is called before the datastore is initialized in
    *                               {@link #run(Configuration, Environment)}
    */
   public Datastore getDatastore() {
      if (morphiaDatastore == null) {
         throw new IllegalStateException("Could not access datastore before Application#run(Configuration, Environment).");
      }
      return morphiaDatastore;
   }

   private MongoClient createClient(Environment environment, MongoConfiguration mongoConfiguration) {
      return new MongoClientBuilder(mongoConfiguration).build(environment);
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
       * @param entityClass A model class that represents an entity. Using explicit classes instead of scanning packages
       *                    boosts application startup.
       */
      default DisableConverterBuilder<C> withEntity(Class<?> entityClass) {
         return withEntities(entityClass);
      }

      /**
       * @param entityClasses Model classes that represent entities. Using explicit classes instead of scanning packages
       *                     boosts application startup.
       */
      default DisableConverterBuilder<C> withEntities(Class<?>... entityClasses) {
         return withEntities(asList(entityClasses));
      }

      /**
       * @param entityClasses Model classes that represent entities. Using explicit classes instead of scanning packages
       *                      boosts application startup.
       */
      DisableConverterBuilder<C> withEntities(@NotNull Iterable<Class<?>> entityClasses);

      /**
       * @param packageToScanForEntities The package that should be scanned for entities recursively.
       */
      DisableConverterBuilder<C> withEntityScanPackage(@NotNull String packageToScanForEntities);

      /**
       * @param markerClass A class or interface that defines the base package for recursive entity scanning. The class
       *                    may be a marker interface or a specific entity class.
       */
      default DisableConverterBuilder<C> withEntityScanPackageClass(Class markerClass) {
         return withEntityScanPackage(markerClass.getPackage().getName());
      }

   }

   public interface DisableConverterBuilder<C extends Configuration> extends CustomConverterBuilder<C> {

      /**
       * Disables the default {@link TypeConverter}s defined in {@link MorphiaBundle#DEFAULT_CONVERTERS}.
       */
      CustomConverterBuilder<C> disableDefaultTypeConverters();

   }

   public interface CustomConverterBuilder<C extends Configuration> extends FinalBuilder<C> {

      /**
       * Adds a custom {@link TypeConverter}s, see
       * {@link org.mongodb.morphia.converters.Converters#addConverter(TypeConverter)}
       * @param typeConverters the converters to add
       */
      CustomConverterBuilder<C> addCustomTypeConverters(Iterable<TypeConverter> typeConverters);

      /**
       * Adds a custom {@link TypeConverter}s, see
       * {@link org.mongodb.morphia.converters.Converters#addConverter(TypeConverter)}
       * @param typeConverters the converters to add
       */
      default CustomConverterBuilder<C> addCustomTypeConverters(TypeConverter... typeConverters) {
         return addCustomTypeConverters(asList(typeConverters));
      }

      /**
       * Adds a custom {@link TypeConverter}, see
       * {@link org.mongodb.morphia.converters.Converters#addConverter(TypeConverter)}
       * @param typeConverter the converter to add
       */
      default CustomConverterBuilder<C> addCustomTypeConverter(TypeConverter typeConverter) {
         return addCustomTypeConverters(singletonList(typeConverter));
      }

   }

   public interface FinalBuilder<C extends Configuration> extends ScanPackageBuilder<C> {


      /**
       * Builds the mongo bundle
       * @return mongo bundle
       */
      MorphiaBundle<C> build();
   }

   public static class Builder<T extends Configuration>
         implements InitialBuilder, ScanPackageBuilder<T>, DisableConverterBuilder<T>, CustomConverterBuilder<T>, FinalBuilder<T> {

      private final MongoConfigurationProvider<T> configProvider;
      private final Set<TypeConverter> customConverters = new LinkedHashSet<>(DEFAULT_CONVERTERS);
      private final Set<String> packagesToScan = new HashSet<>();
      private final Set<Class> entityClasses = new HashSet<>();

      private Builder() {
         configProvider = null;
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
      public DisableConverterBuilder<T> withEntities(Iterable<Class<?>> entityClasses) {
         entityClasses.forEach(this.entityClasses::add);
         return this;
      }

      @Override
      public DisableConverterBuilder<T> withEntityScanPackage(String packageToScanForEntities) {
         packagesToScan.add(Validate.notBlank(packageToScanForEntities));
         return this;
      }

      @Override
      public CustomConverterBuilder<T> disableDefaultTypeConverters() {
         this.customConverters.clear();
         return this;
      }

      @Override
      public CustomConverterBuilder<T> addCustomTypeConverters(Iterable<TypeConverter> typeConverter) {
         typeConverter.forEach(customConverters::add);
         return this;
      }

      @Override
      public MorphiaBundle<T> build() {
         return new MorphiaBundle<>(configProvider, packagesToScan, entityClasses, customConverters);
      }
   }
}
