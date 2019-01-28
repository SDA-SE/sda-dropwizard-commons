# SDA Commons Server Morphia

This module is an example how to use the [morphia bundle](../sda-commons-server-morphia/README.md).

The application creates a connection to a mongo database and stores [`cars`](./src/main/java/org/sdase/commons/server/morphia/example/mongo/model/Car.java) into a collection.
The [`CarsManager`](./src/main/java/org/sdase/commons/server/morphia/example/mongo/CarManager.java) encapsulates the access methods to the datastore.

The example also shows how the datastore object (created within the bundle) can be used in other classes by dependency injection (WELD).

The demonstration integration test [`MorphiaApplicationIT`](./src/integTest/java/org/sdase/commons/server/morphia/example/MorphiaApplicationIT.java) shows
how to use the `MongoDBRule` and the `LazyRule` to create a WELD capable Dropwizard application that uses a mongo database in a test case. 

 

 