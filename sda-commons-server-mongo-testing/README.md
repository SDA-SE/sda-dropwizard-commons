# SDA Commons Server Mongo Testing

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-mongo-testing/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-mongo-testing)

This module provides the [`MongoDbRule`](src/main/java/org/sdase/commons/server/mongo/testing/MongoDbRule.java), 
a JUnit test rule that is used to automatically bootstrap a MongoDB instance for integration tests.

This is accomplished using [Flapdoodle embedded MongoDB](https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo), 
that downloads and starts MongoDB in a separate process. 
As an alternative, one can use [FakeMongo Fongo](https://github.com/fakemongo/fongo) to create an 
in-memory MongoDB database, however it seems to be discontinued and is lacking major features.

## Usage

To use the test rule, a dependency to this module has to be added:

```
testCompile 'org.sdase.commons:sda-commons-server-mongo-testing:<current-version>'
```

To create a MongoDB instance, add the MongoDB test rule to your test class:

```
@ClassRule
public static final MongoDbRule RULE = MongoDbRule
      .builder()
      .withDatabase(DATABASE_NAME)
      .withUsername(DATABASE_USERNAME)
      .withPassword(DATABASE_PASSWORD)
      .build();
```

The test rule takes care to choose a free port for the database. You can access the database 
servers address using `RULE.getHost()`.
Often one need to pass the server address to the constructor of another rule, where the 
[`LazyRule`](../sda-commons-server-testing/src/main/java/org/sdase/commons/server/testing/LazyRule.java) 
can be handy:

```
private static final MongoDbRule MONGODB = MongoDbRule.builder().build();

private static final LazyRule<DropwizardAppRule<AppConfiguration>> DW =
    new LazyRule<>(
        () ->
            new DropwizardAppRule<>(
                MyApplication.class,
                ResourceHelpers.resourceFilePath("test-config.yml"),
                ConfigOverride.config("mongo.hosts", MONGODB.getHost())));

@ClassRule
public static final RuleChain CHAIN = RuleChain.outerRule(MONGODB).around(DW);
```

The rule also provides a `RULE.clearDatabase()` method to remove everything or the `RULE.clearCollections()` 
method to remove all documents from the database between tests, without restarting the rule. 
To verify and modify the database during tests, `RULE.createClient()` provides a way to access the
database using the `MongoClient`.

### Http Proxy

If the `http_proxy` environment variable is present, the configured proxy is used to download the 
mongod executable.

### Scripting

By default, scripting using JavaScript is disabled.
You should avoid using it, as it can cause security issues.
If you still need to use it, activate it using the build `enableScripting()`.

### Operating Systems and MongoDB versions

Flapdoodles embedded MongoDB version < 4.x may result in 
`java.lang.IllegalStateException: java.io.IOException: Could not start process: <EOF>` during start-up 
of a MongoDB server instance. Therefore the operating system is determined and the default MongoDB 
version is set. On Windows systems the version will be set to 4.x and on all other system it will be
3.6.x. If one needs a specific version the version can be set like this `MongoDbRule.builder().withVersion(specificMongoDbVersion).build()`.

This is a temporary solution until all systems run on MongoDB >= version 4.x.

### Configuration in a special CI-environment

Normally the mongod executable is downloaded directly from the mongodb web page.
However in some CI-environments this behavior might be undesired, because of proxy servers, missing 
internet access, or to avoid downloading executables from untrusted sources.
 
Therefor it is possible to change the download location of the embedded mongod using the optional 
environment variable `EMBEDDED_MONGO_DOWNLOAD_PATH`.
If `EMBEDDED_MONGO_DOWNLOAD_PATH` is set to `http://example.com/download/`, the rule for example 
tries to download `http://example.com/download/osx/mongodb-osx-ssl-x86_64-3.6.5.tgz`.

