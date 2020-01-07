package org.sdase.commons.server.hibernate;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.hibernate.ScanningHibernateBundle;
import io.dropwizard.hibernate.SessionFactoryFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.hibernate.SessionFactory;

import javax.validation.constraints.NotNull;

public class HibernateBundle<C extends Configuration> implements ConfiguredBundle<C> {

   private static final String POSTGRES_DRIVER_CLASS = "org.postgresql.Driver";
   private static final Map<String, String> DEFAULT_PROPERTIES = initDefaultProperties();

   private static Map<String, String> initDefaultProperties() {
      Map<String, String> defaults = new HashMap<>();
      defaults.put("charSet", "UTF-8");
      defaults.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
      defaults.put("currentSchema", "public");
      return defaults;
   }

   private io.dropwizard.hibernate.HibernateBundle<C> delegate;

   private HibernateBundle(Set<String> packagesToScanForEntities, DatabaseConfigurationProvider<C> configurationProvider) {
      String[] packagesToScan = packagesToScanForEntities.toArray(new String[]{});
      delegate = new ScanningHibernateBundle<C>(packagesToScan, new SessionFactoryFactory()) {

         @Override
         public PooledDataSourceFactory getDataSourceFactory(C configuration) {
            DataSourceFactory database = configurationProvider.apply(configuration);
            applyDefaultSettings(database);
            return database;
         }
      };
   }

   private HibernateBundle(List<Class<?>> entityClasses, DatabaseConfigurationProvider<C> configurationProvider) {
      delegate = new NonScanningHibernateBundle<C>(entityClasses, new SessionFactoryFactory()) {

         @Override
         public PooledDataSourceFactory getDataSourceFactory(C configuration) {
            DataSourceFactory database = configurationProvider.apply(configuration);
            applyDefaultSettings(database);
            return database;
         }
      };
   }

   private void applyDefaultSettings(DataSourceFactory database) {
      if (database.getDriverClass() == null) {
         database.setDriverClass(POSTGRES_DRIVER_CLASS);
      }

      if (database.getProperties() == null) {
         database.setProperties(new LinkedHashMap<>());
      }
      DEFAULT_PROPERTIES.entrySet().stream()
            .filter(e -> !database.getProperties().containsKey(e.getKey()))
            .forEach(e -> database.getProperties().put(e.getKey(), e.getValue()));
   }

   public static InitialBuilder builder() {
      return new Builder();
   }

   public SessionFactory sessionFactory() {
      return delegate.getSessionFactory();
   }

   @Override
   public void initialize(Bootstrap<?> bootstrap) {
      delegate.initialize(bootstrap);
   }

   @Override
   public void run(C configuration, Environment environment) throws Exception {
      delegate.run(configuration, environment);
      environment.jersey().register(this);
   }


   //
   // Builder
   //

   public interface InitialBuilder {
      /**
       * @param configurationProvider the method reference that provides the {@link DataSourceFactory} from the
       *                              applications configuration class
       */
      <T extends Configuration> ScanPackageBuilder<T> withConfigurationProvider(
            @NotNull DatabaseConfigurationProvider<T> configurationProvider);
   }

   public interface ScanPackageBuilder<T extends Configuration> {
      /**
       * @param packageToScanForEntities The package that should be scanned for entities recursively.
       */
      FinalBuilder<T> withEntityScanPackage(@NotNull String packageToScanForEntities);

      /**
       * @param markerClass A class or interface that defines the base package for recursive entity scanning. The class
       *                    may be a marker interface or a specific entity class.
       */
      FinalBuilder<T> withEntityScanPackageClass(@NotNull Class<?> markerClass);

      /**
       * @param entityClasses The entity classes
       */
      FinalBuilder<T> withEntityClasses(@NotNull Class<?>... entityClasses);
   }

   public interface FinalBuilder<T extends Configuration> extends ScanPackageBuilder<T> {
      HibernateBundle<T> build();
   }

   public static class Builder<T extends Configuration>
         implements InitialBuilder, ScanPackageBuilder<T>, FinalBuilder<T> {

      private Set<String> packagesToScan = new LinkedHashSet<>();

      private DatabaseConfigurationProvider<T> configurationProvider;

      private List<Class<?>> entityClasses;

      private Builder() {
      }

      private Builder(DatabaseConfigurationProvider<T> configurationProvider) {
         this.configurationProvider = configurationProvider;
      }

      @Override
      public <C extends Configuration> ScanPackageBuilder<C> withConfigurationProvider(
            DatabaseConfigurationProvider<C> configurationProvider) {
         return new Builder<>(configurationProvider);
      }

      @Override
      public FinalBuilder<T> withEntityScanPackage(@NotNull String packageToScanForEntities) {
         packagesToScan.add(Validate.notBlank(packageToScanForEntities));
         return this;
      }

      public FinalBuilder<T> withEntityClasses(@NotNull Class<?>... entityClasses) {
         Validate.notEmpty(entityClasses);
         this.entityClasses = Arrays.asList(entityClasses);
         return this;
      }

      @Override
      public FinalBuilder<T> withEntityScanPackageClass(@NotNull Class markerClass) {
         return withEntityScanPackage(markerClass.getPackage().getName());
      }

      @Override
      public HibernateBundle<T> build() {
         return packagesToScan != null && !packagesToScan.isEmpty() ?
             new HibernateBundle<>(packagesToScan, configurationProvider) :
             new HibernateBundle<>(entityClasses, configurationProvider);
      }
   }
}
