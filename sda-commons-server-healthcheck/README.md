# SDA Commons Server Healthcheck

The SDA platform distinguishes two kinds of health checks
* _internal_: health checks for the internal status of the applications. Meaning, if such an health check reports unhealthy, the problem might be fixed by restarting the service
* _external_: a health check that monitors components the application relies on, e.g. a failed database connection or dependent service  

Dropwizard, by default, does not support this distinction. It provides all health checks on the endpoint `/healthcheck`. This is also the case for SDA applications. 

This module extends the Dropwizard default by provides means to introduce _internal_ and _external_ health checks: 
* the [`InternalHealthCheckEndpointBundle`](src/main/java/org/sdase/commons/server/healthcheck/bundle/InternalHealthCheckEndpointBundle.java) that publishes a new endpoint `/healthcheck/internal`. 
  This endpoint only considers _internal_ health checks. The response status is _500_ if at least one _internal_ health check reports unhealthy. 
* the marker interface [`ExternalHealthCheck`](src/main/java/org/sdase/commons/server/healthcheck/bundle/ExternalHealthCheck.java) to mark an health check as _external_
* the [`ExternalServiceHealthCheck`](src/main/java/org/sdase/commons/server/healthcheck/helper/ExternalServiceHealthCheck.java) that is a configurable implementation to check if a dependent
 service is healthy by sending a __GET__ request and validating the response status.   