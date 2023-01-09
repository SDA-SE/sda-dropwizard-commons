# SDA Commons Server Spring Data Mongo

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-spring-data-mongo/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-spring-data-mongo)

The module [`sda-commons-server-spring-data-mongo`](./README.md) is used to work
with MongoDB using [Spring Data Mongo](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/).

## Initialization

The [`SpringDataMongoBundle`](./src/main/java/org/sdase/commons/server/spring/data/mongo/SpringDataMongoBundle.java)
should be added as a field in the application class instead of being anonymously added in the initialize
method like other bundles of this library.

The Dropwizard application's config class needs to provide a
[`SpringDataMongoConfiguration`](./src/main/java/org/sdase/commons/server/spring/data/mongo/SpringDataMongoConfiguration.java).

Please refer to the official documentation how to annotate your entity classes correctly, e.g. by
adding `@Document`, `@MongoId` or `@Indexed`.

```java
public class MyApp extends Application<MyConfiguration> {

  private final SpringDataMongoBundle<MyConfiguration> springDataMongoBundle =
      SpringDataMongoBundle.builder()
          .withConfigurationProvider(MyConfiguration::getSpringDataMongo)
          .withEntites(MyEntity.class)
          .build();

  private PersonRepository personRepository;

  @Override
  public void initialize(Bootstrap<MyConfiguration> bootstrap) {
    bootstrap.addBundle(springDataMongoBundle);
  }

  @Override
  public void run(MyConfiguration configuration, Environment environment) {
    personRepository = springDataMongoBundle.createRepository(PersonRepository.class);
  }

  public MongoOperations getMongoOperations() {
    return springDataMongoBundle.getMongoOperations();
  }

  public PersonRepository getPersonRepository() {
    return personRepository;
  }
}
```

## Configuration

The database connection is configured in the `config.yaml` of the application. We recommend to use 
the [`connectionString`](https://www.mongodb.com/docs/manual/reference/connection-string/) to 
configure your database connection.

```yaml
mongo:
  connectionString: "${MONGODB_CONNECTION_STRING:-}"
```

Example config for **developer** machines using [local-infra](https://github.com/SDA-SE/local-infra):
```yaml
mongo:
  connectionString: "mongodb://mongo-1:27118,mongo-2:27119,mongo-3:27120/myAppName?replicaSet=sda-replica-set-1"
```

In tests the config is derived from the `MongoDbClassExtension`. See
[`sda-commons-server-mongo-testing`](../sda-commons-server-mongo-testing/README.md) for details.

### Legacy support

Application migration from sda-dropwizard-commons 2 that still used the Morphia bundle can also
use the following configuration properties:

```yaml
mongo:
  connectionString: "${MONGODB_CONNECTION_STRING:-}"
  hosts: "${MONGODB_HOSTS:-}"
  database: "${MONGODB_DATABASE:-}"
  options: "${MONGODB_OPTIONS:-}"
  username: "${MONGODB_USERNAME:-}"
  password: "${MONGODB_PASSWORD:-}"
  useSsl: ${MONGODB_USE_SSL:-true}
```

## Inheritance in Entities

It is strongly recommended to annotate all types that are used in a field that does not exactly
match the type with `@TypeAlias`.
Using `@TypeAlias` will replace the default class name as discriminator with the given value in the
annotation and gives you the ability for refactoring of the model classes.
This rule applies for all types that are a subclass of an (abstract) super class, types that are
stored in a field defined as `Object` and all types that are stored in a shared collection.
The latter are usually a subtype of an abstract class to support a common repository.

It is important to register each class that is annotated with `@TypeAlias` by using `withEntities`
in the builder of the bundle.
If not registered there, the mapping is unknown when reading entities.

## Health check

A health check with the name _mongo_ is automatically registered to test the mongo connection.
A simple _ping_ command to the database is used.

## Index creation

The bundle will create indexes automatically by default. You can change the configuration using
the builder:

```java
private final SpringDataMongoBundle<MyConfiguration> springDataMongoBundle =
  SpringDataMongoBundle.builder()
      .withConfigurationProvider(MyConfiguration::getSpringDataMongo)
      .withEntites(MyEntity.class)
      .disableAutoIndexCreation()
      .build();
```

## Enabling validation
You can use the `javax.validation.constraints` annotations to validate your beans e.g: [`Person`](./src/test/java/org/sdase/commons/server/spring/data/mongo/example/model/Person.java).
The mongo bean validation will be disabled by default. You can enable it using the builder:

```java
private final SpringDataMongoBundle<MyConfiguration> springDataMongoBundle =
  SpringDataMongoBundle.builder()
      .withConfigurationProvider(MyConfiguration::getSpringDataMongo)
      .withValidation()
      .build();
```

## Spring Data Mongo Repositories

The bundle support creating Spring Data Mongo repositories that are defined by an interface. You
can create an instance of your repository using the bundle's `createRepository` method that
accepts the interface.

```java
public interface PersonRepository extends PagingAndSortingRepository<Person, ObjectId> {

  // additional custom finder methods go here
}
```

```java
var personRepository = springDataMongoBundle.createRepository(PersonRepository.class);
```

## CA Certificates support

Instead of providing `caCertificate` as an environment variable, mount the CA certificates in PEM format
in the directory `/var/trust/certificates`. Certificates available in sub-directories will also be loaded.

Note that this directory is also configurable through the Dropwizard config class. The config class should then provide a
[`CaCertificateConfiguration`](../sda-commons-shared-certificates/src/main/java/org/sdase/commons/shared/certificates/ca/CaCertificateConfiguration.java)
to the bundle builder. See [`sda-commons-shared-certificates`](../sda-commons-shared-certificates/README.md) for details.
