# SDA Commons Starter Example

This example module shows an 
[application](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-starter-example/src/main/java/org/sdase/commons/starter/example/SdaPlatformExampleApplication.java) that uses the 
[`SdaPlatformBundle`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-starter/src/main/java/org/sdase/commons/starter/SdaPlatformBundle.java)
to bootstrap a Dropwizard application for use in the SDA Platform.

Beside the initialization of the bundle, it includes a 
[REST endpoint](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-starter-example/src/main/java/org/sdase/commons/starter/example/people/rest/PersonEndpoint.java) to demonstrate
registration of endpoints to map resources.

The 
[integration test](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-starter-example/src/test/java/org/sdase/commons/serr/starter/example/SdaPlatformExampleApplicationIT.java) 
shows how the application is bootstrapped in tests. The tests show the capabilities of a standard platform application.

The provided [`local-config.yaml`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-starter-example/local-config.yaml) allows to start the 
[application](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-starter-example/src/main/java/org/sdase/commons/starter/example/SdaPlatformExampleApplication.java) without the 
need for authentication locally using a run configuration of the favourite IDE that defines the program arguments 
`server sda-commons-starter-example/local-config.yaml`. Note that there will be no data available and the example
application does not provide POST endpoints. All that is available is an empty array at `GET /people` and the simple
Swagger documentation at `GET /swagger.json` or `GET /swagger.yaml`

The [`config.yaml`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-starter-example/config.yaml) is an example how the application can be started in production. Such file should be 
copied in the Docker container so that the variables can be populated using the environment configured by the 
orchestration tool (e.g. Kubernetes).   