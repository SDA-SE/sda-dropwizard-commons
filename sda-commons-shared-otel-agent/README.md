# SDA Commons Shared OpenTelemetry agent

This provides an extension of the [OpenTelemetry Java agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation) that redefines the default configuration that are set in the base agent to match requirements of Sda services.
All configuration that can be used can be found in [OpenTelemetry SDK Autoconfigure](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure). 

> **_NOTE:_** This module _must_ be built as a fat jar.