# SDA Commons Server Hibernate Example

The module provides an example [application](https://github.com/SDA-SE/sda-dropwizard-commons/tree/master/sda-commons-server-hibernate-example/src/main/java/org/sdase/commons/server/hibernate/example/HibernateExampleApplication.java) 
on how to use the [`HibernateBundle`](./server-hibernate.md).

The application provides a simple [REST endpoint](https://github.com/SDA-SE/sda-dropwizard-commons/tree/master/sda-commons-server-hibernate-example/src/main/java/org/sdase/commons/server/hibernate/example/rest/PersonsEndPoint.java) to demonstrate the creation of a
transactional context using the `@UnitOfWork` annotation. More details can be found on [the Dropwizard Hibernate documentation page](https://www.dropwizard.io/1.0.0/docs/manual/hibernate.html)
It also comprises two example models, one for REST resources and one for the hibernate entity model.

Manager objects that encapsulates the db access can be tested separately in Unit tests as shown in 
[this](https://github.com/SDA-SE/sda-dropwizard-commons/tree/master/sda-commons-server-hibernate-example/src/test/java/org/sdase/commons/server/hibernate/example/test/PersonManagerTest.java) example.  

   