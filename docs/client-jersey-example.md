# SDA Commons Client Jersey Example

This example module shows an [application](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-client-jersey-example/src/main/java/org/sdase/commons/client/jersey/JerseyClientExampleApplication.java)
that uses the client-jersey bundle to invoke another service.

Beside the initialization of the bundle, it includes the generation of three different clients:
* Generic Platform client
* Generic External client
* External client using an interface that describes the API to generate a proxy
* Generic External client with custom configurations 

The [integration test](https://github.com/SDA-SE/sda-dropwizard-commons/tree/main/sda-commons-client-jersey-example//src/test/java/org/sdase/commons/client/jersey/JerseyClientExampleIT.java) shows
how to set up a mock using the [`sda-commons-client-jersey-wiremock-testing`](./client-jersey-wiremock-testing.md) 
module.

Check the [documentation](./client-jersey.md) for more details.
