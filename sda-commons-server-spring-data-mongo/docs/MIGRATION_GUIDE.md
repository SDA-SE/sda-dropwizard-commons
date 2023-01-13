# Migration Guide

## Getting started
The spring-data-mongo package is available at the following coordinates:
```groovy
implementation "org.sdase.commons:sda-commons-server-spring-data-mongo"
```

Replacing the MorphiaBundle:

```diff
-  private final MorphiaBundle<AppConfiguration> morphiaBundle =
-    MorphiaBundle.builder()
-      .withConfigurationProvider(AppConfiguration::getMongo)
-      .withEntityScanPackageClass(ConsentConfigurationEntity.class)
-      .withValidation()
-      .build();

+  private final SpringDataMongoBundle<AppConfiguration> mongoBundle = 
+    SpringDataMongoBundle.builder()
+      .withConfigurationProvider(AppConfiguration::getMongo)
+      .build();
```

Morphia's `Datastore` can be replaced by Spring Data Mongo's `MongoOperations`:
```java
public MongoOperations getMongoOperations() {
  return this.mongoBundle.getMongoOperations();
}
```

### Morphia compatibility
When upgrading a service from 2.x.x as set of converters is provided to stay compatible with a
database that was initialised with Morphia. To enable these converters, `.withMorphiaCompatibility()`
can be used when building the SpringDataMongoBundle.

```java
SpringDataMongoBundle.builder()
  .withConfigurationProvider(AppConfiguration::getSpringDataMongo)
  .withMorphiaCompatibility()
  build();
```

### Validation
Automatic JSR validation is no longer provided in v3. If you still want to validate your models
you can do so manually using the `ValidationFactory`:

```java
boolean isValid =
  Validation.buildDefaultValidatorFactory()
    .getValidator()
    .validate(myEntity)
    .isEmpty();
```

## Queries

### Building queries with the datastore
Simple operations can be realised by the MongoOperations directly.

Saving an entity:
```java
public void save(SampleEntity entity) {
    mongoOperations.save(entity);
```

Find by id:
```java
public Optional<SampleEntity> findById(String id) {
    return Optional.ofNullable(mongoOperations.findById(id, SampleEntity.class));
```

More complex queries can be realised by building a compound `Query`:
```java
public Optional<SampleEntity> findByArbitraryFields(String identifier, String value) {
  return Optional.ofNullable(
    mongoOperations.findOne(
      new Query(
        where(EntityFields.ARBITRARY_FIELD).is(identifier))
          .and(EntityFields.OTHER_FIELD).ne(value),
        SampleEntity.class));
}
```
See the [official documentation](https://docs.spring.io/spring-data/mongodb/docs/current/api/org/springframework/data/mongodb/core/query/Criteria.html) for further information.

### Auto-generate queries out of method names with the `MongoRepository` interface
Depending on the complexity, queries can also be auto-generated based on the method names by using
the MongoRepository Interface provided by spring-data-mongo.

Imagine the following simple entity class:
```java
public class Person {
  private String Name;
  private int age;
}
```

Now we can define queries directly through the method names (see the official documentation for the specific syntax).
```java
public interface PersonMongoRepository extends MongoRepository<Person, String> {
  Optional<Person> findByName(String name);

  List<Person> findAllByAgeIsLessThanEqual(int age);

  List<Person> findAllByNameIsNot(String name);
}
```
See the [official documentation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods.details) for further details.
