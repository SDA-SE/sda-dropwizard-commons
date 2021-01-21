# SDA Commons Server Morphia Example

This module is an example how to use the [morphia bundle](../sda-commons-server-morphia/README.md).

The application creates a connection to a mongo database and stores [`cars`](./src/main/java/org/sdase/commons/server/morphia/example/mongo/model/Car.java) into a collection.
The [`CarsManager`](./src/main/java/org/sdase/commons/server/morphia/example/mongo/CarManager.java) encapsulates the access methods to the datastore.

The example also shows how the datastore object (created within the bundle) can be used in other classes by dependency injection (WELD).

This module contains the following tests:

| Test | JUnit 4 | JUnit 5 | Weld |
|------|---------|---------|------|
| [`MorphiaApplicationJUnit4IT`](./src/test/java/org/sdase/commons/server/morphia/example/MorphiaApplicationJUnit4IT.java) | Yes | No | No |
| [`MorphiaApplicationJUnit5IT`](./src/test/java/org/sdase/commons/server/morphia/example/MorphiaApplicationJUnit5IT.java) | No | Yes | No |
| [`MorphiaApplicationWeldIT`](./src/test/java/org/sdase/commons/server/morphia/example/MorphiaApplicationWeldIT.java) | Yes | No | Yes |

Note: The applications are not meant to be started. They're only used for integration test purposes.
