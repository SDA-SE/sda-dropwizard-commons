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

The [`HibernateBundle`](./src/main/java/org/sdase/commons/server/hibernate/HibernateBundle.java) should be added as 
field in the application class instead of being anonymously added in the initialize method like other bundles of this 
library. Implementations need to refer to the instance to get access to the `SessionFactory`.

The Dropwizard applications config class needs to provide a `DataSourceFactory`.

The bundle builder requires to define the getter of the `DataSourceFactory` as method reference to access the 
configuration. One or more packages that should be scanned for entity classes must be defined. 

Entity classes should be described using 
[JPA annotations](https://docs.jboss.org/hibernate/annotations/3.5/reference/en/html/entity.html). The packages to scan 
may be either added as String or can be derived from a class or marker interface in the root of the model packages. 

```java
public class MyApplication extends Application<MyConfiguration> {
   
   private final HibernateBundle<HibernateITestConfiguration> hibernateBundle = HibernateBundle.builder()
                          .withConfigurationProvider(MyConfiguration::getDatabase)
                          .withEntityScanPackageClass(MyEntity.class)
                          .build();
   
   // ...
   
}
```

In the context of a CDI application, the `SessionFactory` instance that is created in the `HibernateBundle` should be
provided as CDI bean so it can be injected into managers, repositories or however the data access objects are named in 
the application:

```java
@ApplicationScoped
public class MyCdiApplication extends Application<MyConfiguration> {
   
   private final HibernateBundle<HibernateITestConfiguration> hibernateBundle = HibernateBundle.builder()
                          .withConfigurationProvider(MyConfiguration::getDatabase)
                          .withEntityScanPackageClass(MyEntity.class)
                          .build();
   
   public static void main(final String[] args) throws Exception {
      // from sda-commons-server-weld
      DropwizardWeldHelper.run(SolutionServiceApplication.class, args);
   }

   // ...
   
   @javax.enterprise.inject.Produces
   public SessionFactory sessionFactory() {
      return hibernateBundle.sessionFactory();
   }

}

public class MyEntityManager extends io.dropwizard.hibernate.AbstractDAO<MyEntity> {

   @Inject
   public MyEntityManager(SessionFactory sessionFactory) {
      super(sessionFactory);
   }
   
   // ....
}
```

### Configuration

The database connection is configured in the `config.yaml` of the application. It uses the format of 
[Dropwizard Hibernate](https://www.dropwizard.io/1.3.5/docs/manual/hibernate.html). Defaults are defined for PostgreSQL.
The default PostgreSQL schema is `public`. The key (_database_ in the examples) depends on the applications 
configuration class.

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

For database schema migration, [Flyway](https://flywaydb.org/) is used by the 
[`DbMigrationService`](./src/main/java/org/sdase/commons/server/hibernate/DbMigrationService.java) and works with the
same `DataSourceFactory` as the `HibernateBundle`. It may be used in a custom `ConfiguredCommand` in each application.
Therefore defaults for the command name and the documentation is provided:

```java
public class DbMigrationCommand extends ConfiguredCommand<MyAppConfig> {

   public DbMigrationCommand() {
      super(DbMigrationService.DEFAULT_COMMAND_NAME, DbMigrationService.DEFAULT_COMMAND_DOC);
   }

   @Override
   protected void run(Bootstrap<MyAppConfig> bootstrap, Namespace namespace, MyAppConfig configuration) {
      new DbMigrationService(configuration.getDatabase()).migrateDatabase();
   }
}
```

The command needs to be added to the application:
```java
public class HibernateApp extends Application<MyAppConfig> {
   @Override
   public void initialize(Bootstrap<MyAppConfig> bootstrap) {
      // ...
      bootstrap.addCommand(new DbMigrationCommand());
      // ...
   }
   // ...
}
```

The command is then executed from command line to migrate the database:
```
java -jar my-application.jar migrateDB ./config.yaml
```

DB migration scripts are expected in `src/main/resources/db.migration/*.sql`.

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
    testCompile 'org.sdase.commons:sda-commons-server-hibernate-testing:<VERSION>'
```

For creating tests without a Dropwizard application please refer to the 
[`DbMigrationServiceTest`](./src/test/java/org/sdase/commons/server/hibernate/DbMigrationServiceTest.java) as example.

For creating full integration tests of a Dropwizard application please refer to
[`HibernateIT`](./src/integTest/java/org/sdase/commons/server/hibernate/HibernateIT.java) as example. 
