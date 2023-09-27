# SDA Commons Server OpenAPI Example

This example module shows an 
[application](https://github.com/SDA-SE/sda-dropwizard-commons/tree/master/sda-commons-server-openapi-example/src/main/java/org/sdase/commons/server/openapi/example/OpenApiExampleApplication.java) that uses the 
[`OpenApiBundle`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/master/sda-commons-server-openapi/src/main/java/org/sdase/commons/server/openapi/OpenApiBundle.java)
to describe REST endpoints with a OpenApi 3 documentation.

Beside the initialization of the bundle via the [`SdaPlatformBundle`](https://github.com/SDA-SE/sda-dropwizard-commons/blob/master/sda-commons-starter/src/main/java/org/sdase/commons/starter/SdaPlatformBundle.java),
it includes a [`PersonService`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/master/sda-commons-server-openapi-example/src/main/java/org/sdase/commons/server/openapi/example/people/rest/PersonService.java) 
and a [`PersonResource`](https://github.com/SDA-SE/sda-dropwizard-commons/tree/master/sda-commons-server-openapi-example/src/main/java/org/sdase/commons/server/openapi/example/people/rest/PersonResource.java)
to demonstrate some cases of API documentation.

The 
[integration test](https://github.com/SDA-SE/sda-dropwizard-commons/tree/master/sda-commons-server-openapi-example/src/test/java/org/sdase/commons/server/openapi/example/people/rest/OpenApiIT.java) 
shows how the existence of a OpenAPI endpoint can be tested.

The provided [`local-config.yaml`](https://github.com/SDA-SE/sda-dropwizard-commons/blob/master/sda-commons-server-openapi-example/src/test/resources/test-config.yaml) allows to start the 
[application](https://github.com/SDA-SE/sda-dropwizard-commons/tree/master/sda-commons-server-openapi-example/src/main/java/org/sdase/commons/server/openapi/example/OpenApiExampleApplication.java) without the 
need for authentication locally using a run configuration of the favourite IDE that defines the program arguments 
`server sda-commons-server-openapi-example/local-config.yaml`.
Swagger documentation is available at `GET /openapi.json` or `GET /openapi.yaml`, 
you may use the [Swagger Editor](https://editor.swagger.io) or [Swagger UI](http://petstore.swagger.io/) to view the documentation. 

The [`config.yaml`](https://github.com/SDA-SE/sda-dropwizard-commons/blob/master/sda-commons-server-openapi-example/config.yaml) is an example how the application can be started in production. Such file should be 
copied in the Docker container so that the variables can be populated using the environment configured by the 
orchestration tool (e.g. Kubernetes).
