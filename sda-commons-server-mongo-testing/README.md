# SDA Commons Server Mongo Testing

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-mongo-testing/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-mongo-testing)

This module provides the [`MongoDbClassExtension`](src/main/java/org/sdase/commons/server/mongo/testing/MongoDbClassExtension.java),
a JUnit 5 test extension that is used to automatically bootstrap a MongoDB instance for integration tests.

This is accomplished using [Flapdoodle embedded MongoDB](https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo),
that downloads and starts MongoDB in a separate process.

## Usage

To create a MongoDB instance, add the MongoDB test extension to your test class:

```java
@RegisterExtension
@Order(0)
static final MongoDbClassExtension MONGO_DB_EXTENSION = MongoDbClassExtension
      .builder()
      .withDatabase(DATABASE_NAME)
      .withUsername(DATABASE_USERNAME)
      .withPassword(DATABASE_PASSWORD)
      .build();
```

The test extension takes care to choose a free port for the database. You can access the database
servers address using `MONGO_DB_EXTENSION.getHosts()`.
Often one need to pass the server address to the constructor of another extension:

```java
import io.dropwizard.testing.junit5.DropwizardAppExtension;

public class PersistenceIT {

  @RegisterExtension
  @Order(0)
  static final MongoDbClassExtension MONGO_DB_EXTENSION = MongoDbClassExtension.builder().build();

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<MyApplicationConfiguration> DW =
      new DropwizardAppExtension<>(
          MyApplication.class,
          ResourceHelpers.resourceFilePath("test-config.yml"),
          ConfigOverride.config("mongo.hosts", MONGO_DB_EXTENSION::getHosts));
}
```

The extension also provides a `MONGO_DB_EXTENSION.clearDatabase()` method to remove everything or the `MONGO_DB_EXTENSION.clearCollections()`
method to remove all documents from the database between tests, without restarting the extension.
To verify and modify the database during tests, `MONGO_DB_EXTENSION.createClient()` provides a way to access the
database using the `MongoClient`.


### HTTP Proxy

If the `http_proxy` environment variable is present, the configured proxy is used to download the
`mongod` executable.

### Scripting

By default, scripting using JavaScript is disabled.
You should avoid using it, as it can cause security issues.
If you still need to use it, activate it using the build `enableScripting()`.

### MongoDB version

Flapdoodles embedded MongoDB version is set to 4.4.x by default.
If one needs a specific version the version can be set like this
`MongoDbClassExtension.builder().withVersion(specificMongoDbVersion).build()`.

### Configuration in a special CI-environment

Normally the `mongod` executable is downloaded directly from the MongoDB web page.
However in some CI-environments this behavior might be undesired, because of proxy servers, missing
internet access, or to avoid downloading executables from untrusted sources.

Therefore it is possible to change the download location of the embedded `mongod` using the optional
environment variable `EMBEDDED_MONGO_DOWNLOAD_PATH`.
If `EMBEDDED_MONGO_DOWNLOAD_PATH` is set to `http://example.com/download/`, the extension for example
tries to download `http://example.com/download/osx/mongodb-osx-ssl-x86_64-3.6.5.tgz`.

### Use an Existing Database

**Experimental Feature**

To test specific scenarios, e.g. a real database set up like in production, the extension can be
configured to not bootstrap a database with Flapdoodle.

A [MongoDB Connection String](https://docs.mongodb.com/manual/reference/connection-string/) must be
set as environment variable `TEST_MONGODB_CONNECTION_STRING` in the build environment to reference
the existing database:

```bash
# example for SDA SE internal MongoDB in local-infra
export TEST_MONGODB_CONNECTION_STRING=mongodb://<user>:<password>@mongo-1:27118,mongo-2:27119,mongo-3:27120/testdb?authSource=admin
./gradlew check
``` 

It may be required to create a Keystore and apply it to JVM that executes the tests if the database
uses custom certificates.

If such an environment variable is set, the `MongoDbClassExtension` will not start a local MongoDB but
provides the configuration and an appropriate `MongoClient` to access the external database.

To make this feature work, tests

- must provide all configuration to the application in test as shown in the [example above](#usage)
- must clean up the collections they modify because a single database is shared across tests
- cannot run in parallel because only a single database is shared across tests
