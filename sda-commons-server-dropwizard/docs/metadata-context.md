# The Metadata Context


## Purpose

The metadata context keeps contextual information of a business process across all involved
microservices.
The information that is stored in the metadata context is solely defined by the environment.
Defining a metadata context for an environment is optional.  
If no context is defined, no metadata will be available for any service.

The metadata should not affect the core business logic of a service but can be used to affect how a
service communicates with the user (e.g. communication methods, templates or domains used for links)
or how statistical data is evaluated.
As the metadata is tracked for a whole business process across all involved services, it supports
that services which are not in a synchronous call chain, can behave differently based on the entry
point.
All services in between and the service that uses information from the metadata context don't need
to clutter their API with non-functional information, that may be different in each environment, to
support features a service in the end of a process provides based on the initial user request.

A service must be able to fulfill its purpose without any information from the metadata context.
A service may use information from the metadata context to use or apply different business related
or non-functional configuration for a specific business process but must have a default for these
configurations as well.

The metadata context is technically transferred via HTTP headers and Kafka message headers between
services in the same environment.
At runtime, it is held in a thread local variable to be available for the current process.

[The example below](#example-template-selection) explains what kind of problems the metadata context
can solve.


## Configuration and structure

A metadata context consists of well-defined fields.
They must be defined equally for all services in the same environment.
Services based on this library use the environment variable `METADATA_FIELDS` as comma separated
list of field names automatically.
Field names are used as HTTP and Kafka message header keys.
The fields must not conflict with officially defined header keys.

The data type of the value is always text, represented as `String` in this library.
Multiple values are supported for each field in a specific context.
To simplify parsing, according to
[RFC 9110 5.2](https://www.rfc-editor.org/rfc/rfc9110.html#name-field-lines-and-combined-fi), values
must not contain commas.


## Creating metadata context information

A metadata context for a specific business process should be initialized as early as possible.
Technically the context is created by passing the headers of defined metadata fields to a service
that supports the metadata context.

No standard component should create a metadata context initially, because the metadata context
always depends on the environment.

The easiest way to initially create a metadata context is to add the respective headers in a proxy
that exposes a service, e.g. an Ingress in Kubernetes.
The proxy may derive the metadata information from the used domain, query params, user agent
headers and other information available in the request.

If getting the metadata information is more complex, an environment specific interaction service
(according to the SDA service architecture) may be implemented and create the context
programmatically.
This service should act like a proxy and forward the request to a standard business service.

Services that recreate a context for a process that was interrupted, use the context they stored
along with the entity to activate it for current thread.


## Support metadata context in a service

Any service that communicates synchronously with the HTTP platform clients provided by this
library and asynchronously with Kafka consumers and producers initialized by this library and has no
persistence layer most likely supports the metadata context.
Exceptions may apply if business processes are interrupted or asynchronously processed without
saving business data in a database.

Services that persist data and therefore interrupt the business process until the data is loaded
into memory again can support the metadata context if they store it along with the entity and
recreate the runtime context when the entity is loaded and processed.

Service that interrupts a process in memory or proceeds asynchronously must transfer the metadata
context to the new thread to support the metadata context.

Services that fulfill these requirements should mention the support of the metadata context in their
deployment documentation.
They should also mention the configuration environment variable `METADATA_FIELDS` that is
automatically available for all services based on this library.

Services that read information from the metadata context shall not use hard coded field names.
They must not expect that specific fields are available in an environment.
The environment, technically the operations team, is in the lead to define which metadata is
available.

A service only knows a specific purpose for which it supports metadata information.
Whenever a service supports decisions based on metadata configuration, it must provide a
configuration option which naming is based on the purpose to let the operations team define which
metadata field should be used.
The library supports to derive the actual field names from environment variables.
The values are dependent on the environment as well.
Therefore, the service must provide configuration options to map values of the metadata field to
values it understands.

Supporting information from the metadata context to distinguish the behavior of the service can be
very complex due to the loose coupling and the high flexibility for the owners of the environment.
Every configuration that can be modified by information of the metadata context must have a default
as well, because the service may be used in an environment without configured metadata context or in
a specific business process is no context available. 
How a service behaves and is configured when metadata context is available must be described in the
deployment documentation to make this complex flexibility manageable by the operations team.


## Example: Template selection

Imagine a business case that is advertised on different landing pages, where the user enters
information and receives a result asynchronously via email.
The email sent to the user should match the style of the landing page.

Each landing page is available on a dedicated domain but all lead to the same backend services.
The services may communicate asynchronously or even need additional information from the backoffice
team to create the result.

The operations team defines the `METADATA_FIELDS` as `landing-page-source` for all services in the
environment to identify the landing page where the user entered their data.
They configure the Ingress, to set either `christmas-special` or `summer-edition` as
`landing-page-source` header - based on the domain of two landing pages.

No service but the one that is generating the email is affected by this information.
The core business rules that determine the result only need the information the user entered.
The email content can be created independently as well.

Only the service that renders the HTML part of the email must be aware of the source of the request.
Therefore, it must have the ability to use multiple templates.
Besides other optional input to determine the template to use, it must be configurable to use
information from the metadata context.

To keep the flexibility the metadata context offers, it defines the environment variable
`TEMPLATE_HINT_METADATA_FIELD`.
The operations team configures it as `landing-page-source`.
The service developer can use `TEMPLATE_HINT_METADATA_FIELD` in the
[`MetadataContext`](../src/main/java/org/sdase/commons/server/dropwizard/metadata/MetadataContext.java)
API to get the values of current context, but must be aware that the result is empty or contains
multiple values.

Additionally, the email templating service must provide configuration options to select a template
based on the value as well as providing the templates.

The templates and the configuration could be mounted as files in the container.
The latter may be a yaml file and look like this:

```yaml
# templates.yaml
default: /mounts/templates/default.hbs
byMetadata:
  - metadataValue: christmas-special
    templateFile: /mounts/templates/xmas.hbs
  - metadataValue: summer-edition
    templateFile: /mounts/templates/summer.hbs
```


### Variant of template selection

The email example is very flexible and would support other environments as well, e.g. when
A/B testing is used to find the perfect color for the use case.
The email service is not involved in the evaluation of the A/B test, but has to send emails that
match the styles of the A/B test.
Such user tests will most likely be evaluated by dedicated UX tools and not by the backend services
in the environment.
No business service should be affected by this information.
However, a service creating statistics for the use case could support the metadata context as well
and provide additional evaluation for each variant.

The frontend must provide any information in the request to distinguish between A and B.
Then the proxy can convert the information in a header that is sent to the backend service, e.g.
`ab-variant=neon`.

The operations team of this environment will configure `METADATA_FIELDS` for all services and
`TEMPLATE_HINT_METADATA_FIELD` as `ab-variant` and the email templating like this:

```yaml
# templates.yaml
default: /mounts/templates/default.hbs
byMetadata:
  - metadataValue: pastel
    templateFile: /mounts/templates/pastel.hbs
  - metadataValue: neon
    templateFile: /mounts/templates/neon.hbs
```
