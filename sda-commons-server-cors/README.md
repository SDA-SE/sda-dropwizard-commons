# SDA Commons Server CORS
This module provides the functionality to announce one or more hostnames to the client on the server side so that they can be added as exception(s) to the SOP (Same-Origin-Policy).
## Configuration


The CorsFiltering contains the parameter ALLOWED_ORIGINS_PARAM in the CrossOriginFilter. As part of the values there is a environment variable with the same name. This environment variable will be defined in the config.yml in the root directory of the application, which uses the SDA Commons libraries.


CorsFiltering.java
```
        filter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "http://localhost:*,${ALLOWED_ORIGINS_PARAM}");
```

config.yml
```
server:
  rootPath: "/api/*"
  cors:
    allowedOrigins: ${CORS_ALLOWED_ORIGINS}
```

This variable will be set in the kubernetes.yml to set the "whitelist" of hostnames.
