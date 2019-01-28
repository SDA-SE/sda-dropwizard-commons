# SDA Commons Server Morphia

The module [`sda-commons-server-morphia`](./sda-commons-server-morphia/README.md) is used to work
with MongoDB using [Morphia](https://github.com/MorphiaOrg).

## Usage

### Initialization

The [`MorphiaBundle`](./src/main/java/org/sdase/commons/server/morphia/MorphiaBundle.java) should be added as 
field in the application class instead of being anonymously added in the initialize method like other bundles of this 
library. Implementations need to refer to the instance to get access to the `Datastore`.

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

