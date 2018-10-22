# SDA Commons Server Hibernate

The module `sda-commons-server-hibernate` provides access to relational databases with hibernate. It is primarily
designed to use PostgreSQL in production and H2 in memory in tests. 

It provides 
- configuration of the database to use,
- db migration using [Flyway](https://flywaydb.org/),
- a health check for the database connection

`sda-commons-server-hibernate` ships with `org.postgresql:postgresql:42.1.1` and `org.flywaydb:flyway-core:4.2.0` at
compile scope.

## Usage

### Initialization

The [`HibernateBundle`](./src/main/java/com/sdase/commons/server/hibernate/HibernateBundle.java) should be added as 
field in the application class instead of being anonymously added in the initialize method like other bundles of this 
library. Implementations need to refer to the instance to get access to the `SessionFactory`. _Note that there may be 
some differences when using Weld as CDI implementation. Extending documentation for this scenario will be added when
Weld joins this library. This task is addressed in [SEBP-490](https://sda-se.atlassian.net/browse/SEBP-490)._

The Dropwizard applications config class needs to extend Dropwizard's `Configuration` and implement the 
[`DatabaseConfigurable`](./src/main/java/com/sdase/commons/server/hibernate/DatabaseConfigurable.java) interface.

The bundle builder requires to define the configuration class and one or more packages that should be scanned for
entity classes. Entity classes should be described using JPA annotations. The packages to scan may be either added as 
String or can be derived from a class or marker interface in the root of the model packages. 

```java
public class MyApplication extends Application<MyConfiguration> {
   
   private final HibernateBundle<HibernateITestConfiguration> hibernateBundle = HibernateBundle.builder()
                          .withConfigClass(MyConfiguration.class)
                          .withEntityScanPackageClass(MyEntity.class)
                          .build();
   
   // ...
   
}
```

### Configuration

The database connection is configured in the `config.yaml` of the application. It uses the format of 
[Dropwizard Hibernate](https://www.dropwizard.io/1.3.5/docs/manual/hibernate.html). Defaults are defined for PostgreSQL.
The default PostgreSQL schema is `public`.

Example config for **production**:
```yaml
database:
  user: username
  password: s3cr3t
  url: jdbc:postgresql://postgres-host:5432/my_service
```

Example config for **developer** machines using [local-infra](https://github.com/SDA-SE/local-infra):
```yaml
database:
  user: dbuser
  password: sda123
  url: jdbc:postgresql://localhost:5435/postgres
  properties:
    currentSchema: my_service
```

Example config for **testing**:
```yaml
database:
  driverClass: org.h2.Driver
  user: sa
  password: sa
  url: jdbc:h2:mem:test;DB_CLOSE_DELAY=-1
  properties:
    hibernate.dialect: org.hibernate.dialect.H2Dialect
```

### Database access

DAOs or repositories are built by extending the `AbstractDAO`. The 
[Dropwizard Hibernate Documentation](https://www.dropwizard.io/1.3.5/docs/manual/hibernate.html#data-access-objects)
has an example. The required `SessionFactory` is provided by the `HibernateBundle` instance.

### Schema migration

For database schema migration, [Flyway](https://flywaydb.org/) is wrapped in a 
[Dropwizard Command](https://www.dropwizard.io/1.3.5/docs/manual/core.html#man-core-commands). The command is 
registered as `migrateDB` and is executed from the command line:

```
java -jar my-application.jar migrateDB ./config.yaml
```

DB migration scripts are expected in `src/main/resources/db.migration/*.sql` and 

### Health check

A health check with the name _hibernate_ is automatically registered to test the connect to the database. By default
`SELECT 1` is used as test query. It can be customized in the `config.yaml`:

```yaml
database:
  validationQuery: SELECT 2
``` 

## Testing

For testing database access with Hibernate we suggest to use [sda-commons-hibernate-testing](../sda-commons-server-hibernate-testing/README.md) module.

Dependencies to be added:

```
    testCompile 'com.sdase.commons:sda-commons-server-hibernate-testing:<VERSION>'
```

For creating tests without a Dropwizard application please refer to the 
[DbMigrationCommandTest](./src/test/java/com/sdase/commons/server/hibernate/DbMigrationCommandTest.java) as example.

For creating full integration tests of a Dropwizard application please refer to
[HibernateIT](./src/integTest/java/com/sdase/commons/server/hibernate/HibernateIT.java) as example. 
