# SDA Commons BOM

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-bom/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-bom)

The module `sda-commons-bom` defines the versions of sda-commons modules.

## Usage

> ##### [Gradle 5 required!](https://gradle.org/whats-new/gradle-5/)

Services using sda-commons can import `sda-commons-bom` to make sure to use the same 
dependencies. After that you can use all modules of sda-commons **without** declaring the version.
Example for your `build.gradle`:

```
dependencies {
    compile enforcedPlatform("org.sdase.commons:sda-commons-bom:$sdaCommonsVersion")
    compile enforcedPlatform("org.sdase.commons:sda-commons-dependencies:$sdaCommonsVersion")
    compile "org.sdase.commons:sda-commons-starter"
    compile "org.sdase.commons:sda-commons-server-morphia"
    compile "org.sdase.commons:sda-commons-server-kafka"
    compile "org.sdase.commons:sda-commons-server-s3"
    ...
}
```

or if you prefer Gradle's `implementation`:

```
dependencies {
    implementation enforcedPlatform("org.sdase.commons:sda-commons-bom:$sdaCommonsVersion")
    implementation enforcedPlatform("org.sdase.commons:sda-commons-dependencies:$sdaCommonsVersion")
    implementation "org.sdase.commons:sda-commons-starter"
    ...
}
```
