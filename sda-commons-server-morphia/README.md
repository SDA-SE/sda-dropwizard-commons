# SDA Commons Server Morphia

The module [`sda-commons-server-morphia`](./README.md) is used to work
with MongoDB using [Morphia](https://github.com/MorphiaOrg).

## Usage

### Initialization

The [`MorphiaBundle`](./src/main/java/org/sdase/commons/server/morphia/MorphiaBundle.java) should be added as a
field in the application class instead of being anonymously added in the initialize method like other bundles of this 
library. Implementations need to refer to the instance to access the `Datastore`.

The Dropwizard applications config class needs to provide a 
[`MongoConfiguration`](./src/main/java/org/sdase/commons/server/morphia/MongoConfiguration.java).

The bundle builder requires to define the getter of the `MongoConfiguration` as method reference to access the 
configuration. One or more entity classes should be defined. Entities must be declared on initialization to ensure that
Morphia creates the defined indices on the collection of the entity. Entities may be added by class path scanning as
well.

Entity classes should be described using 
[Morphia Annotations](http://morphiaorg.github.io/morphia/1.4/guides/annotations/). When class path scanning is used,
the `Entity` annotation is required. All entities need an `Id` field. 

```java
public class MyApplication extends Application<MyConfiguration> {
   
   private MorphiaBundle<Config> morphiaBundle = MorphiaBundle.builder()
         .withConfigurationProvider(MyConfiguration::getMongo)
         .withEntity(MyEntity.class)
         .build();
   
   @Override
   public void initialize(Bootstrap<Config> bootstrap) {
      bootstrap.addBundle(morphiaBundle);
   }

   // ...
   
}
```

Support for JSR-303 **validation** (e.g. `@NotNull`) is provided by calling 
`withValidation` when creating the bundle with the builder:

```
  MorphiaBundle.builder()
    .withConfigurationProvider(Config::getMongo)
    .withEntity(Person.class)
    .withValidation()
    .build();
```

#### Dependency Injection

In the context of a CDI application, the `Datastore` instance that is created in the `MorphiaBundle` should be
provided as CDI bean so it can be injected into managers, repositories or however the data access objects are named in 
the application:

```java
@ApplicationScoped
public class MyCdiApplication extends Application<MyConfiguration> {
   
   private MorphiaBundle<Config> morphiaBundle = MorphiaBundle.builder()
     .withConfigurationProvider(MyConfiguration::getMongo)
     .withEntity(MyEntity.class)
     .build();
   
   @Override
   public void initialize(Bootstrap<Config> bootstrap) {
      bootstrap.addBundle(morphiaBundle);
   }

   // ...
   
   @javax.enterprise.inject.Produces
   public Datastore datastore() {
      return morphiaBundle.datastore();
   }

}
```

## Configuration

The database connection is configured in the `config.yaml` of the application.

Example config for **production** to be used with environment variables of the cluster configuration:
```yaml
mongo:
  hosts: ${MONGODB_HOSTS}
  database: ${MONGODB_DATABASE:-}
  options: ${MONGODB_OPTIONS:-}
  username: ${MONGODB_USERNAME:-}
  password: ${MONGODB_PASSWORD:-}
  useSsl: ${MONGODB_USE_SSL:-true}
  caCertificate: ${MONGODB_CA_CERTIFICATE}
```

Example config for **developer** machines using [local-infra](https://github.com/SDA-SE/local-infra):
```yaml
database:
  hosts: mongo-1:27118,mongo-2:27119,mongo-3:27120
  options: replicaSet=sda-replica-set-1
  database: myAppName
  useSsl: false
```

In tests the config is derived from the `MongoDbRule`. See 
[sda-commons-server-mongo-testing](../sda-commons-server-mongo-testing/README.md) for details.


### Health check

A health check with the name _mongo_ is automatically registered to test the mongo connection. 
A simple _ping_ command to the database is used.


## Testing

For testing database access with Morphia we suggest to use 
[sda-commons-mongo-testing](../sda-commons-server-mongo-testing) module.
