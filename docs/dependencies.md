# SDA Commons dependencies

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-dependencies/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-dependencies)

The module `sda-commons-dependencies` defines the versions of sda-commons' dependencies. This 
concept is mostly known as either [platform](https://docs.gradle.org/current/userguide/java_platform_plugin.html) 
or "bill of materials". 

## Usage

[Gradle 5 required!](https://gradle.org/whats-new/gradle-5/)

Services using sda-commons should import `sda-commons-dependencies` to make sure to use the same 
dependencies. Add the following code to your `build.gradle`:

```
dependencies {
    compile enforcedPlatform("org.sdase.commons.sda-commons-dependencies:$sdaCommonsVersion")
}
```

After that you can use all dependencies that were already declared in `sda-commons-dependencies` 
**without** declaring the version.