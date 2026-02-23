# Migration Guide from v8 to v9

## Dropwizard 5

One of the major changes was the update from Dropwizard 4 to 5.

Most notable change is the upgrade to [Jetty 12](#jetty-12).

See the [upgrade notes](https://www.dropwizard.io/en/latest/manual/upgrade-notes/upgrade-notes-5_0_x.html) for more information.

## Jetty 12

We upgraded to jetty 12.
A major change is that the servlet components aren’t part of the Jetty core anymore. 
This commons uses the `jetty-ee10-servlet` components.

Additionally, servlet filter need to be updated to the servlet handler api.

See the [migration guide](https://jetty.org/docs/jetty/12.1/programming-guide/migration/11-to-12.html) for more insights.

The `org.eclipse.jetty.servlets.CrossOriginFilter` header can now be accessed through `org.sdase.commons.server.cors.CorsHeader`.

## Weld 6

Weld was upgraded from 5 to 6. 
Biggest change in combination with dropwizard is, that Beans are no longer autoloaded into the application context.
Instead of
```
environment.getApplicationContext().addServlet(TestServlet.class, "/foo");
```
it is now necessary to register the bean directly
```
TestServlet servlet = CDI.current().select(TestServlet.class).get();
environment.getApplicationContext().addServlet(servlet, "/foo");
```

See also the release notes from [RC-1](https://weld.cdi-spec.org/news/2024/11/13/weld-600CR1/) and [RC-2](https://weld.cdi-spec.org/news/2024/11/28/weld-600CR2/)

## Prometheus

We migrated from Prometheus `simpleclient` to `prometheus-metrics-core`.

`simpleclient`, `simpleclient_dropwizard` and `simpleclient_servlet_jakarta` were replaced with `prometheus-metrics-core`, `prometheus-metrics-instrumentation-dropwizard` and `prometheus-metrics-exporter-servlet-jakarta`.

See the [official documentation](https://prometheus.github.io/client_java/migration/simpleclient/) for migrating. 

Metrics don't have a closing `,` any longer.

`apache_http_client_request_duration_seconds{manager="HttpClient",method="get",name="myClient",quantile="0.5",}`
is now:
`apache_http_client_request_duration_seconds{manager="HttpClient",method="get",name="myClient",quantile="0.5"}`