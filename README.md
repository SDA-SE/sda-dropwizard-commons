# SDA Commons

SDA Commons is a set of libraries to bootstrap services easily that follow the patterns and specifications promoted by
the SDA SE.

SDA Commons is separated in different modules that can be combined as needed. Most of the modules require the 
technologies that are recommended for services in the SDA SE Platform. These technologies include

- [Dropwizard](https://www.dropwizard.io)
- [Jackson](https://github.com/FasterXML/jackson)
- [JAX-RS](https://jcp.org/en/jsr/detail?id=339) 
- [Jersey](https://jersey.github.io/)
- [Swagger](https://swagger.io/)
- [Hibernate](http://hibernate.org/)


## Modules in SDA Commons

### Server

All modules prefixed with `sda-commons-server-` provide technology and configuration support used in backend services
to provide REST Endpoints.


#### Dropwizard

The module [`sda-commons-server-dropwizard`](./sda-commons-server-dropwizard/README.md) provides 
`io.dropwizard:dropwizard-core` with convergent dependencies. All other SDA Commons Server modules use this dependency
and are aligned to the versions provided by `sda-commons-server-dropwizard`.


#### Testing

The module [`sda-commons-server-testing`](./sda-commons-server-testing/README.md) is the base module to add unit and 
integrations test for applications in the SDA SE infrastructure.


#### Jackson

The module [`sda-commons-server-jackson`](./sda-commons-server-jackson/README.md) is used to configure the 
`ObjectMapper` with the recommended default settings of SDA SE services. It also provides support for linking resources 
with HAL and adds the ability to filter fields on client request.


#### Swagger

Coming soon


#### Hibernate

Coming soon 