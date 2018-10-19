package com.sdase.commons.server.hibernate;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.hibernate.ScanningHibernateBundle;
import io.dropwizard.hibernate.SessionFactoryFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.apache.commons.lang3.Validate;
import org.hibernate.SessionFactory;

import javax.validation.constraints.NotNull;
import java.util.*;

public class HibernateBundle<T extends Configuration & DatabaseConfigurable> implements ConfiguredBundle<T> {

   private static final String POSTGRES_DRIVER_CLASS = "org.postgresql.Driver";
   private static final Map<String, String> DEFAULT_PROPERTIES = initDefaultProperties();

   private static Map<String, String> initDefaultProperties() {
      Map<String, String> defaults = new HashMap<>();
      defaults.put("charSet", "UTF-8");
      defaults.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
      defaults.put("currentSchema", "public");
      return defaults;
   }

   private ScanningHibernateBundle<T> delegate;

   private HibernateBundle(Set<String> packagesToScanForEntities) {
      String[] packagesToScan = packagesToScanForEntities.toArray(new String[]{});
      delegate = new ScanningHibernateBundle<T>(packagesToScan, new SessionFactoryFactory()) {
         @Override
         public PooledDataSourceFactory getDataSourceFactory(T configuration) {
            DataSourceFactory database = configuration.getDatabase();
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
      bootstrap.addCommand(new DbMigrationCommand());
   }

   @Override
   public void run(T configuration, Environment environment) throws Exception {
      delegate.run(configuration, environment);
      environment.jersey().register(this);
   }


   //
   // Builder
   //

   public interface InitialBuilder {
      /**
       *
       * @param configurationClass the mandatory configuration class of the application
       */
      <T extends Configuration & DatabaseConfigurable> ScanPackageBuilder<T> withConfigClass(
            @NotNull Class<T> configurationClass);
   }

   public interface ScanPackageBuilder<T extends Configuration & DatabaseConfigurable> {
      /**
       * @param packageToScanForEntities The package that should be scanned for entities recursively.
       */
      FinalBuilder<T> withEntityScanPackage(@NotNull String packageToScanForEntities);

      /**
       * @param markerClass A class or interface that defines the base package for recursive entity scanning. The class
       *                    may be a marker interface or a specific entity class.
       */
      FinalBuilder<T> withEntityScanPackageClass(@NotNull Class<?> markerClass);
   }

   public interface FinalBuilder<T extends Configuration & DatabaseConfigurable> extends ScanPackageBuilder<T> {
      HibernateBundle<T> build();
   }

   public static class Builder<T extends Configuration & DatabaseConfigurable>
         implements InitialBuilder, ScanPackageBuilder<T>, FinalBuilder<T> {

      private Set<String> packagesToScan = new LinkedHashSet<>();

      private Builder() {
      }

      @Override
      public <C extends Configuration & DatabaseConfigurable> ScanPackageBuilder<C> withConfigClass(
            @NotNull Class<C> configurationClass) {
         return new Builder<>();
      }

      @Override
      public FinalBuilder<T> withEntityScanPackage(@NotNull String packageToScanForEntities) {
         packagesToScan.add(Validate.notBlank(packageToScanForEntities));
         return this;
      }

      @Override
      public FinalBuilder<T> withEntityScanPackageClass(@NotNull Class markerClass) {
         return withEntityScanPackage(markerClass.getPackage().getName());
      }

      @Override
      public HibernateBundle<T> build() {
         return new HibernateBundle<>(packagesToScan);
      }
   }
}
