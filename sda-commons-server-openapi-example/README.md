# SDA Commons Server OpenAPI Example

This example module shows an 
[application](src/main/java/org/sdase/commons/server/openapi/example/OpenApiExampleApplication.java) that uses the 
[`OpenApiBundle`](../sda-commons-server-openapi/src/main/java/org/sdase/commons/server/openapi/OpenApiBundle.java)
to describe REST endpoints with a OpenApi 3 documentation.

Beside the initialization of the bundle via the [`SdaPlatformBundle`](../sda-commons-starter/src/main/java/org/sdase/commons/server/starter/SdaPlatformBundle.java),
it includes a [`PersonService`](src/main/java/org/sdase/commons/server/openapi/example/people/rest/PersonService.java) 
and a [`PersonResource`](src/main/java/org/sdase/commons/server/openapi/example/people/rest/PersonResource.java)
to demonstrate some cases of API documentation.

The 
[integration test](src/test/java/org/sdase/commons/server/openapi/example/people/rest/OpenApiIT.java) 
shows how the existence of a OpenAPI endpoint can be tested.

The provided [`local-config.yaml`](local-config.yaml) allows to start the 
[application](src/main/java/org/sdase/commons/server/openapi/example/OpenApiExampleApplication.java) without the 
need for authentication locally using a run configuration of the favourite IDE that defines the program arguments 
`server sda-commons-server-openapi-example/local-config.yaml`.
Swagger documentation is available at `GET /openapi.json` or `GET /openapi.yaml`, 
you may use the [Swagger Editor](https://editor.swagger.io) or [Swagger UI](http://petstore.swagger.io/) to view the documentation. 

The [`config.yaml`](config.yaml) is an example how the application can be started in production. Such file should be 
copied in the Docker container so that the variables can be populated using the environment configured by the 
orchestration tool (e.g. Kubernetes).
