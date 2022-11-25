package org.sdase.commons.server.spring.data.mongo;

import static java.util.Arrays.asList;
import static org.sdase.commons.server.dropwizard.lifecycle.ManagedShutdownListener.onShutdown;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.sdase.commons.server.spring.data.mongo.converter.ZonedDateTimeReadConverter;
import org.sdase.commons.server.spring.data.mongo.converter.ZonedDateTimeWriteConverter;
import org.sdase.commons.server.spring.data.mongo.converter.morphia.compatibility.CharArrayReadConverter;
import org.sdase.commons.server.spring.data.mongo.converter.morphia.compatibility.CharArrayWriteConverter;
import org.sdase.commons.server.spring.data.mongo.converter.morphia.compatibility.LocalDateReadConverter;
import org.sdase.commons.server.spring.data.mongo.converter.morphia.compatibility.LocalDateWriteConverter;
import org.sdase.commons.server.spring.data.mongo.converter.morphia.compatibility.UriReadConverter;
import org.sdase.commons.server.spring.data.mongo.health.MongoHealthCheck;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.Jsr310Converters;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactoryBean;
import org.springframework.data.repository.Repository;

public class SpringDataMongoBundle<C extends Configuration> implements ConfiguredBundle<C> {

  /** Custom converters to stay compatible with sda-spring-boot-commons for new services. */
  private static final Set<Converter<?, ?>> DEFAULT_CONVERTERS =
      new LinkedHashSet<>(
          List.of(
              // ZonedDateTime
              ZonedDateTimeWriteConverter.INSTANCE, ZonedDateTimeReadConverter.INSTANCE));

  /** Custom converters to stay compatible with Morphia for services that upgrade from 2.x.x. */
  private static final Set<Converter<?, ?>> CONVERTERS_MORPHIA_COMPATIBILITY =
      new LinkedHashSet<>(
          List.of(
              // char[]
              CharArrayReadConverter.INSTANCE,
              CharArrayWriteConverter.INSTANCE,
              // URI
              UriReadConverter.INSTANCE,
              // LocalDate
              LocalDateWriteConverter.INSTANCE,
              LocalDateReadConverter.INSTANCE,
              // ZonedDateTime
              ZonedDateTimeWriteConverter.INSTANCE,
              ZonedDateTimeReadConverter.INSTANCE));

  private final Function<C, SpringDataMongoConfiguration> configurationProvider;

  private MongoClient mongoClient;

  private MongoOperations mongoOperations;

  private final Set<Converter<?, ?>> customConverters = new LinkedHashSet<>();

  private boolean autoIndexCreation = true;

  private GenericApplicationContext applicationContext;

  private boolean validationEnabled = false;

  private final Set<Class<?>> entityClasses = new HashSet<>();

  private boolean morphiaCompatibilityEnabled = false;

  /**
   * Database as defined by the {@link SpringDataMongoConfiguration#getConnectionString()} or {@link
   * SpringDataMongoConfiguration#getDatabase()}
   */
  private String database;

  public SpringDataMongoBundle(SpringDataMongoConfigurationProvider<C> configurationProvider) {
    this.configurationProvider = configurationProvider;
  }

  public static InitialBuilder builder() {
    return new Builder<>();
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // nothing to do ATM
  }

  @Override
  public void run(C configuration, Environment environment) {
    var config = configurationProvider.apply(configuration);
    if (this.validationEnabled) {
      this.applicationContext = createAndStartApplicationContext();
    }

    String connectionString;
    if (StringUtils.isNotBlank(config.getConnectionString())) {
      connectionString = config.getConnectionString();
    } else {
      connectionString =
          String.format(
              "mongodb://%s:%s@%s/%s",
              config.getUsername(), config.getPassword(), config.getHosts(), config.getDatabase());
      if (StringUtils.isNotBlank(config.getOptions())) {
        connectionString += "?" + config.getOptions();
      }
    }

    var cs = new ConnectionString(connectionString);
    this.database = cs.getDatabase();
    mongoClient = MongoClients.create(cs);

    registerHealthCheck(environment.healthChecks(), mongoClient, this.database);
    registerOnShutdown(environment);
  }

  /**
   * registers a health check for the mongo database
   *
   * @param healthCheckRegistry registry where to register health checks
   * @param database database name that is used within health check
   */
  private void registerHealthCheck(
      HealthCheckRegistry healthCheckRegistry, MongoClient mongoClient, String database) {
    healthCheckRegistry.register(
        "mongo", new MongoHealthCheck(mongoClient.getDatabase((database))));
  }

  /**
   * @return the {@link MongoOperations} that can be used to interact with your MongoDB instance
   */
  public MongoOperations getMongoOperations() {
    if (mongoOperations == null) {
      mongoOperations = createMongoOperations();
    }
    return mongoOperations;
  }

  /**
   * @return a new Spring Data Mongo repository
   * @param repositoryType the type of the repository that is created
   * @param <T> the repository class or interface
   * @param <S> the type of the entity
   * @param <ID> the type of primary key of the entity
   */
  @SuppressWarnings("java:S119") // ID follows Spring Data conventions
  public <T extends Repository<S, ID>, S, ID extends Serializable> T createRepository(
      Class<T> repositoryType) {
    MongoRepositoryFactoryBean<T, S, ID> factoryBean =
        new MongoRepositoryFactoryBean<>(repositoryType);
    factoryBean.setMongoOperations(getMongoOperations());
    factoryBean.afterPropertiesSet();
    return factoryBean.getObject();
  }

  /**
   * creates a mongoTemplate to be used for operations in database. If the option {@code
   * withValidation} was selected in the Bundle creation, it will initialize a spring application
   * context for the validation
   */
  private MongoOperations createMongoOperations() {
    SimpleMongoClientDatabaseFactory mongoDbFactory =
        new SimpleMongoClientDatabaseFactory(mongoClient, this.database);
    MongoConverter mongoConverter = getDefaultMongoConverter(mongoDbFactory, getConverters());
    var mongoTemplate = new MongoTemplate(mongoDbFactory, mongoConverter);
    if (validationEnabled) {
      // we need the application context for the application events (OnBeforeSave) to be supported
      mongoTemplate.setApplicationContext(applicationContext);
    }
    return mongoTemplate;
  }

  /**
   * creates a spring application context to enable validation features, if the option {@code
   * withValidation} was selected in the Bundle creation
   */
  private GenericApplicationContext createAndStartApplicationContext() {
    var context = new GenericApplicationContext();
    context.addApplicationListener(new ValidatingMongoEventListener(createValidator()));
    context.refresh();
    context.start();
    return context;
  }

  /**
   * @return a new Validator instance
   */
  private Validator createValidator() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      return factory.getValidator();
    }
  }

  /** shutdowns the spring application context, if created */
  private void registerOnShutdown(Environment environment) {
    if (applicationContext != null) {
      environment
          .lifecycle()
          .manage(
              onShutdown(
                  () -> {
                    applicationContext.stop();
                    applicationContext.close();
                  }));
    }
  }

  private List<?> getConverters() {
    List<Converter<?, ?>> converters = new ArrayList<>();
    if (morphiaCompatibilityEnabled) {
      converters.addAll(CONVERTERS_MORPHIA_COMPATIBILITY);
    } else {
      converters.addAll(DEFAULT_CONVERTERS);
    }
    converters.addAll(customConverters);
    return converters;
  }

  private SpringDataMongoBundle<C> withEntities(List<Class<?>> entityClasses) {
    this.entityClasses.addAll(new HashSet<>(entityClasses));
    return this;
  }

  private SpringDataMongoBundle<C> addCustomConverters(Collection<Converter<?, ?>> converters) {
    this.customConverters.addAll(converters);
    return this;
  }

  private SpringDataMongoBundle<C> setAutoIndexCreation(boolean autoIndexCreation) {
    this.autoIndexCreation = autoIndexCreation;
    return this;
  }

  private SpringDataMongoBundle<C> setValidationEnabled(boolean validationEnabled) {
    this.validationEnabled = validationEnabled;
    return this;
  }

  private SpringDataMongoBundle<C> setMorphiaCompatibilityEnabled(
      boolean morphiaCompatibilityEnabled) {
    this.morphiaCompatibilityEnabled = morphiaCompatibilityEnabled;
    return this;
  }

  /** Copied from Spring's {@link MongoTemplate} */
  private MongoConverter getDefaultMongoConverter(
      MongoDatabaseFactory factory, List<?> converters) {
    DbRefResolver dbRefResolver = new DefaultDbRefResolver(factory);
    MongoCustomConversions conversions = new MongoCustomConversions(converters);

    MongoMappingContext mappingContext = new MongoMappingContext();
    mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
    mappingContext.setAutoIndexCreation(autoIndexCreation);
    if (this.validationEnabled) {
      mappingContext.setApplicationContext(applicationContext);
      mappingContext.setApplicationEventPublisher(applicationContext);
    }

    mappingContext.setInitialEntitySet(entityClasses);
    mappingContext.afterPropertiesSet();

    MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mappingContext);
    converter.setCustomConversions(conversions);
    converter.setCodecRegistryProvider(factory);
    converter.afterPropertiesSet();
    if (morphiaCompatibilityEnabled) {
      converter.setTypeMapper(new DefaultMongoTypeMapper("className"));
    }

    return converter;
  }

  public interface InitialBuilder {

    /**
     * @param configurationProvider the method reference that provides the @{@link
     *     SpringDataMongoConfiguration} from the applications configurations class
     * @param <C> the type of the applications configuration class
     * @return a builder instance for further configuration
     */
    <C extends Configuration> MorphiaCompatibilityBuilder<C> withConfigurationProvider(
        @NotNull SpringDataMongoConfigurationProvider<C> configurationProvider);
  }

  public interface MorphiaCompatibilityBuilder<C extends Configuration>
      extends ScanPackageBuilder<C> {

    /**
     * Enables compatibility of the data mapping with the defaults provided by the old {@code
     * MorphiaBundle}. This configuration should ONLY be used when upgrading a Service from
     * sda-dropwizard-commons v2.x.x to avoid data migration.
     *
     * <p>It is strongly suggested to use test data reflecting the real structure and formats in the
     * collections for unit tests of the repositories before upgrading from v2.x.x to v3.x.x to test
     * for compatibility when migrating to {@code sda-commons-spring-data-mongo}.
     *
     * <p>Some data types that where supported in Morphia 1.6.x, are <strong>not supported</strong>
     * with sda-commons-server-spring-data-mongo in compatibility mode:
     *
     * <ul>
     *   <li>{@link java.sql.Timestamp} (stored as `date` by Morphia, not mappable)
     *   <li>{@link java.time.LocalTime} (stored as `long` representing nano of day by Morphia, not
     *       mappable)
     *   <li>{@link java.time.LocalDateTime} (technically works, but gaps are possible due to time
     *       zone settings)
     *   <li>{@link com.mongodb.DBRef} (compatibility not tested)
     *   <li>{@code dev.morphia.geo.Geometry} and all its implementations
     * </ul>
     *
     * @see CustomConverterBuilder#addCustomConverters(Converter[]) defaults without Morphia
     *     compatibility
     * @return a builder instance for further configuration
     */
    ScanPackageBuilder<C> withMorphiaCompatibility();
  }

  public interface ScanPackageBuilder<C extends Configuration> extends FinalBuilder<C> {

    /**
     * @param entityClasses Model classes that represent entities. Using explicit classes instead of
     *     scanning packages boosts application startup.
     * @return a builder instance for further configuration
     */
    CustomConverterBuilder<C> withEntities(Class<?>... entityClasses);
  }

  public interface CustomConverterBuilder<C extends Configuration> extends FinalBuilder<C> {

    /**
     * Adds a custom {@link Converter}s
     *
     * <p>By default the bundle provides {@link Jsr310Converters} and custom converters {@linkplain
     * org.sdase.commons.server.spring.data.mongo.converter.ZonedDateTimeWriteConverter to write
     * <code>ZoneDateTime</code> as <code>Date</code>} and {@linkplain
     * org.sdase.commons.server.spring.data.mongo.converter.ZonedDateTimeReadConverter read
     * <code>ZoneDateTime</code> from <code>Date</code>} unless {@linkplain
     * MorphiaCompatibilityBuilder#withMorphiaCompatibility() Morphia compatibility is enabled}.
     *
     * @param converters the converters to add
     * @return a builder instance for further configuration
     */
    CustomConverterBuilder<C> addCustomConverters(Iterable<Converter<?, ?>> converters);

    /**
     * Adds a custom {@link Converter}
     *
     * <p>By default the bundle provides {@link Jsr310Converters} and custom converters {@linkplain
     * org.sdase.commons.server.spring.data.mongo.converter.ZonedDateTimeWriteConverter to write
     * <code>ZoneDateTime</code> as <code>Date</code>} and {@linkplain
     * org.sdase.commons.server.spring.data.mongo.converter.ZonedDateTimeReadConverter read
     * <code>ZoneDateTime</code> from <code>Date</code>} unless {@linkplain
     * MorphiaCompatibilityBuilder#withMorphiaCompatibility() Morphia compatibility is enabled}.
     *
     * @param converters the converters to add
     * @return a builder instance for further configuration
     */
    default CustomConverterBuilder<C> addCustomConverters(Converter<?, ?>... converters) {
      return addCustomConverters(asList(converters));
    }
  }

  public interface FinalBuilder<C extends Configuration> {

    FinalBuilder<C> disableAutoIndexCreation();

    /** enables validation features, otherwise it will be disabled by default */
    FinalBuilder<C> withValidation();

    /**
     * Builds the mongo bundle
     *
     * @return mongo bundle
     */
    SpringDataMongoBundle<C> build();
  }

  public static class Builder<T extends Configuration>
      implements InitialBuilder,
          MorphiaCompatibilityBuilder<T>,
          ScanPackageBuilder<T>,
          CustomConverterBuilder<T>,
          FinalBuilder<T> {

    private SpringDataMongoConfigurationProvider<T> configurationProvider;

    private final Set<Converter<?, ?>> customConverters = new HashSet<>();

    private final List<Class<?>> entityClasses = new ArrayList<>();

    private boolean autoIndexCreation = true;

    private boolean validationEnabled = false;

    private boolean morphiaCompatibilityEnabled = false;

    public Builder(SpringDataMongoConfigurationProvider<T> configurationProvider) {
      this.configurationProvider = configurationProvider;
    }

    public Builder() {}

    @Override
    public <C extends Configuration> MorphiaCompatibilityBuilder<C> withConfigurationProvider(
        SpringDataMongoConfigurationProvider<C> configurationProvider) {
      return new Builder<>(configurationProvider);
    }

    @Override
    public ScanPackageBuilder<T> withMorphiaCompatibility() {
      this.morphiaCompatibilityEnabled = true;
      return this;
    }

    @Override
    public FinalBuilder<T> withValidation() {
      this.validationEnabled = true;
      return this;
    }

    @Override
    public CustomConverterBuilder<T> withEntities(Class<?>... entityClasses) {
      this.entityClasses.addAll(asList(entityClasses));
      return this;
    }

    @Override
    public CustomConverterBuilder<T> addCustomConverters(Iterable<Converter<?, ?>> converters) {
      converters.forEach(customConverters::add);
      return this;
    }

    @Override
    public FinalBuilder<T> disableAutoIndexCreation() {
      this.autoIndexCreation = false;
      return this;
    }

    @Override
    public SpringDataMongoBundle<T> build() {
      return new SpringDataMongoBundle<>(configurationProvider)
          .withEntities(entityClasses)
          .addCustomConverters(customConverters)
          .setAutoIndexCreation(autoIndexCreation)
          .setMorphiaCompatibilityEnabled(morphiaCompatibilityEnabled)
          .setValidationEnabled(validationEnabled);
    }
  }
}
