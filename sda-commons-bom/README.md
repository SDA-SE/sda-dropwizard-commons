# SDA Commons BOM

The module `sda-commons-bom` defines the versions of sda-commons modules.

## Usage

[Gradle 5 required!](https://gradle.org/whats-new/gradle-5/)

Services using sda-commons can import `sda-commons-bom` to make sure to use the same 
dependencies. After that you can use all modules of sda-commons **without** declaring the version.
Example for your `build.gradle`:

```
dependencies {
    compile enforcedPlatform("org.sdase.commons.sda-commons-bom:$sdaCommonsVersion")
    compile enforcedPlatform("org.sdase.commons.sda-commons-dependencies:$sdaCommonsVersion")
    compile "org.sdase.commons:sda-commons-server-starter"
    compile "org.sdase.commons:sda-commons-server-morphia"
    compile "org.sdase.commons:sda-commons-server-kafka"
    compile "org.sdase.commons:sda-commons-server-kafka-confluent"
    compile "org.sdase.commons:sda-commons-server-s3"
    ...
}
```