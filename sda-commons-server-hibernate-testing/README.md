# SDA Commons Server Hibernate Testing

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-hibernate-testing/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-hibernate-testing)

The module `sda-commons-server-hibernate-testing` is the base module to add unit and integration
tests for applications using Hibernate in the SDA SE infrastructure.

This module provides 
* [Database Rider](https://github.com/database-rider/database-rider) 
* [DbUnit](http://dbunit.sourceforge.net/)
* [H2](http://www.h2database.com)

## Further Information

[Database Rider Getting Started](https://database-rider.github.io/getting-started/)

[H2 Features](http://h2database.com/html/features.html)

## DAO Extension

The Junit 5 DAO Extension creates a database and provides means to access it.

To implement a test with a DAO Mock, the `DAOClassExtension` has to be initialized standalone or before `DropwizardAppExtension` implicitly by field declaration order or explicitly with a `@Order(N)`.
The following examples also combines DAO testing with DBUnit testing, which enables you to use  @DataSet or @ExpectedDataSet annotations.

```java
@DBUnit(url = URL, driver = "org.h2.Driver", user = USER, password = PWD)
public class PersonManagerJUnit5IT {

  // define connect information
  static final String URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
  static final String USER = "sa";
  static final String PWD = "sa";

  @Order(0)
  @RegisterExtension
  static final DBUnitExtension dbUnitExtension = new DBUnitExtension();
  
  @Order(1)
  @RegisterExtension
  static final DAOClassExtension daoClassExtension =
      DAOClassExtension.newBuilder()
          .setProperty(AvailableSettings.URL, URL)
          .setProperty(AvailableSettings.USER, USER)
          .setProperty(AvailableSettings.PASS, PWD)
          .setProperty(AvailableSettings.HBM2DDL_AUTO, "create-drop")
          .addEntityClass(PersonEntity.class)
          .build();

   // @Test
}
```
