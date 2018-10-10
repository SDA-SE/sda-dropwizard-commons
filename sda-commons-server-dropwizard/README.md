# SDA Commons Server Dropwizard

`sda-commons-server-dropwizard` is an aggregator module that provides `io.dropwizard:dropwizard-core` with convergent
dependencies.

This module fixes some version mixes of transitive dependencies in dropwizard. Dependency convergence should be checked 
with `gradlew dependencies` on upgrades.