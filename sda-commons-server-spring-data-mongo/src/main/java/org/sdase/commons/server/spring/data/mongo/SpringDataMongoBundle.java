package org.sdase.commons.server.spring.data.mongo;

import static java.util.Arrays.asList;

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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.sdase.commons.server.spring.data.mongo.converter.DateToZonedDateTimeConverter;
import org.sdase.commons.server.spring.data.mongo.converter.LocalDateToStringConverter;
import org.sdase.commons.server.spring.data.mongo.converter.StringToLocalDateConverter;
import org.sdase.commons.server.spring.data.mongo.converter.StringToZonedDateTimeConverter;
import org.sdase.commons.server.spring.data.mongo.converter.ZonedDateTimeToDateConverter;
import org.sdase.commons.server.spring.data.mongo.health.MongoHealthCheck;
import org.springframework.core.convert.converter.Converter;
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
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactoryBean;
import org.springframework.data.repository.Repository;

public class SpringDataMongoBundle<C extends Configuration> implements ConfiguredBundle<C> {

  /**
   * Adding our own JSR 310 converters to stay backwards compatible with the old Morphia Bundle.
   * Spring usually provides {@link org.springframework.data.convert.Jsr310Converters} that are not
   * used here intentionally.
   */
  private static final Set<Converter<?, ?>> DEFAULT_CONVERTERS =
      new LinkedHashSet<>(
          List.of(
              // LocalDate
              new LocalDateToStringConverter(),
              new StringToLocalDateConverter(),
              // ZonedDateTime
              new DateToZonedDateTimeConverter(),
              new StringToZonedDateTimeConverter(),
              new ZonedDateTimeToDateConverter()));

  private final Function<C, SpringDataMongoConfiguration> configurationProvider;

  private SpringDataMongoConfiguration config;

  private MongoClient mongoClient;

  private MongoOperations mongoOperations;

  private final Set<Converter<?, ?>> customConverters = new LinkedHashSet<>();

  private boolean autoIndexCreation = true;

  private Set<Class<?>> entityClasses = new HashSet<>();
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
    this.config = configurationProvider.apply(configuration);

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
   * @param <S> the type of your entity type
   * @param <ID> the type of your primary key
   */
  public <T extends Repository<S, ID>, S, ID extends Serializable> T createRepository(
      Class<T> clazz) {
    MongoRepositoryFactoryBean<T, S, ID> factoryBean = new MongoRepositoryFactoryBean<>(clazz);
    factoryBean.setMongoOperations(getMongoOperations());
    factoryBean.afterPropertiesSet();
    return factoryBean.getObject();
  }

  private MongoOperations createMongoOperations() {
    SimpleMongoClientDatabaseFactory mongoDbFactory =
        new SimpleMongoClientDatabaseFactory(mongoClient, this.database);
    MongoConverter mongoConverter = getDefaultMongoConverter(mongoDbFactory, getConverters());
    return new MongoTemplate(mongoDbFactory, mongoConverter);
  }

  private List<?> getConverters() {
    List<Converter<?, ?>> converters = new ArrayList<>(DEFAULT_CONVERTERS);
    converters.addAll(customConverters);
    return converters;
  }

  private SpringDataMongoBundle<C> withEntity(Class<?> entityClass) {
    withEntities(entityClass);
    return this;
  }

  private SpringDataMongoBundle<C> withEntities(Class<?>... entityClasses) {
    withEntities(asList(entityClasses));
    return this;
  }

  private SpringDataMongoBundle<C> withEntities(List<Class<?>> entityClasses) {
    this.entityClasses.addAll(new HashSet<>(entityClasses));
    return this;
  }

  private SpringDataMongoBundle<C> addCustomConverter(Converter<?, ?> converter) {
    this.customConverters.add(converter);
    return this;
  }

  private SpringDataMongoBundle<C> addCustomConverters(Converter<?, ?>... converters) {
    Arrays.stream(converters).forEach(this::addCustomConverter);
    return this;
  }

  private SpringDataMongoBundle<C> addCustomConverters(Collection<Converter<?, ?>> converters) {
    converters.forEach(this::addCustomConverter);
    return this;
  }

  private SpringDataMongoBundle<C> setAutoIndexCreation(boolean autoIndexCreation) {
    this.autoIndexCreation = autoIndexCreation;
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
    mappingContext.setInitialEntitySet(entityClasses);
    mappingContext.afterPropertiesSet();

    MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mappingContext);
    converter.setCustomConversions(conversions);
    converter.setCodecRegistryProvider(factory);
    converter.afterPropertiesSet();
    converter.setTypeMapper(new DefaultMongoTypeMapper("className"));

    return converter;
  }

  public interface InitialBuilder {

    /**
     * @param configurationProvider the method reference that provides the @{@link
     *     SpringDataMongoConfiguration} from the applications configurations class
     * @param <C> the type of the applications configuration class
     * @return a builder instance for further configuration
     */
    <C extends Configuration> ScanPackageBuilder<C> withConfigurationProvider(
        @NotNull SpringDataMongoConfigurationProvider<C> configurationProvider);
  }

  public interface ScanPackageBuilder<C extends Configuration> extends FinalBuilder<C> {

    /**
     * @param entityClass A model class that represents an entity. Using explicit classes instead of
     *     scanning packages boosts application startup.
     * @return a builder instance for further configuration
     */
    default CustomConverterBuilder<C> withEntity(Class<?> entityClass) {
      return withEntities(entityClass);
    }

    /**
     * @param entityClasses Model classes that represent entities. Using explicit classes instead of
     *     scanning packages boosts application startup.
     * @return a builder instance for further configuration
     */
    default CustomConverterBuilder<C> withEntities(Class<?>... entityClasses) {
      return withEntities(asList(entityClasses));
    }

    /**
     * @param entityClasses Model classes that represent entities. Using explicit classes instead of
     *     scanning packages boosts application startup.
     * @return a builder instance for further configuration
     */
    CustomConverterBuilder<C> withEntities(@NotNull List<Class<?>> entityClasses);
  }

  public interface CustomConverterBuilder<C extends Configuration> extends FinalBuilder<C> {

    /**
     * Adds a custom {@link Converter}s
     *
     * @param converters the converters to add
     * @return a builder instance for further configuration
     */
    CustomConverterBuilder<C> addCustomConverters(Iterable<Converter<?, ?>> converters);

    /**
     * Adds a custom {@link Converter}
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

    /**
     * Builds the mongo bundle
     *
     * @return mongo bundle
     */
    SpringDataMongoBundle<C> build();
  }

  public static class Builder<T extends Configuration>
      implements InitialBuilder, ScanPackageBuilder<T>, CustomConverterBuilder<T>, FinalBuilder<T> {

    private SpringDataMongoConfigurationProvider<T> configurationProvider;

    private final Set<Converter<?, ?>> customConverters = new HashSet<>();

    private final List<Class<?>> entityClasses = new ArrayList<>();

    private boolean autoIndexCreation = true;

    public Builder(SpringDataMongoConfigurationProvider<T> configurationProvider) {
      this.configurationProvider = configurationProvider;
    }

    public Builder() {}

    @Override
    public <C extends Configuration> ScanPackageBuilder<C> withConfigurationProvider(
        SpringDataMongoConfigurationProvider<C> configurationProvider) {
      return new Builder<>(configurationProvider);
    }

    @Override
    public CustomConverterBuilder<T> withEntity(Class<?> entityClass) {
      this.entityClasses.add(entityClass);
      return this;
    }

    @Override
    public CustomConverterBuilder<T> withEntities(Class<?>... entityClasses) {
      this.entityClasses.addAll(asList(entityClasses));
      return this;
    }

    @Override
    public CustomConverterBuilder<T> withEntities(List<Class<?>> entityClasses) {
      this.entityClasses.addAll(entityClasses);
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
          .setAutoIndexCreation(autoIndexCreation);
    }
  }
}
