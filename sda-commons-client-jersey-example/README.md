# SDA Commons Client Jersey Example

This example module shows an [application](./src/main/java/org/sdase/commons/client/jersey/JerseyClientExampleApplication.java)
that uses the client-jersey bundle to invoke another service.

Beside the initialization of the bundle, it includes the generation of three different clients:
* Generic Platform client
* Generic External client
* External client using an interface that describes the API to generate a proxy 

The [integration test](./src/integTest/java/org/sdase/commons/client/jersey/JerseyClientExampleIT.java) shows
how to setup a mock using the [`sda-commons-client-jersey-wiremock-testing`](../sda-commons-client-jersey-wiremock-testing/README.md) 
module. 