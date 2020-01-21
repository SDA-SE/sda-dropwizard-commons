# SDA Commons error handling example

The SDA commons libraries provide support to generate the common error structures as described in the REST guide. This 
implementation is part of the [`sda-commons-server-jackson`](../sda-commons-server-jackson/README.md#error-format) bundle.

The default way to inform clients about errors, exceptions should be thrown. These exceptions are mapped to the 
common error structure using JAX-RS exception mapper (`javax.ws.rs.ext.ExceptionMapper`) automatically, if the Jackson bundle 
is added to the application.

Using a response object (`javax.ws.rs.core.Response`) to inform clients about exceptions will not
necessarily result in the agreed error structure. This is only the case if as entity, an 
[`ApiError`](../sda-commons-shared-error/src/main/java/org/sdase/commons/shared/api/error/ApiError.java)
is used.

This example project shows how the error structure will be generated correctly for the following situations:
* throwing `NotFoundException` as one example for using standard JAX-RS web application exceptions. All of these exceptions are mapped automatically
* throwing an `ApiException`. This might be necessary, if a special response code is required for that no default `WebApplicationException` is defined, e.g. error status 422. 
* returning an error within a response object (not that exception is not logged automatically by mapper)
* returning an error due to validation fault
* returning an error created from a new custom validator
