# SDA Commons Server Health Check Example

This module presents an example 
[application](/src/main/java/org/sdase/commons/server/healthcheck/example/HealthExampleApplication.java) on how to 
create and register health checks.

The application introduces the `InternalHealthCheckEndpointBundle` to create the new endpoint with 
internal health check information only.

It registers one external health check, that checks another service, and an internal health check, that checks the 
state of a simple thread. The thread can be quit with a method call for testing reasons.