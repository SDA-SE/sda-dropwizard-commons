# sda-commons-server-key-mgmt
Bundle for key management in microservices.

The main purpose is to provide configurable key management and mappings for keys and its values.
This allows to define keys and mappings at deployment time and not at development time. 
So the actual keys can be adjusted according to the deployment scenario.

The bundle also provides the annotation `@PlatformKey("<keyName>")` to mark a String attribute to contain a valid key value.

When defining keys at runtime, it might happen that keys used in business logic do not exist at runtime. 
This problem is not solved by the bundle. 
It must be considered as part of the key definition process. 

The bundle provides means to work with keys as well as to retrieve mapping values from or to a possible implementation depending on the keys.
This includes
  * checking if a key is valid
  * checking if a value is valid for a given key
  * mapping of a key between API and implementation specific values

API keys and values should be defined in snake case. For these two values, the bundle will be case tolerant, by mapping to uppercase internally.
For implementations specific values, the bundle must not be case tolerant to provide keys as expected.

The mapping between API and implementation of an API is necessary to define APIs completely independent of a concrete implementation.
For example, when wrapping an API that fits into the platform around an existing implementation.
Therefore, every value mapping consists of an `api` and an `impl`ementation value. 

The bundle provides a "pass through" default behavior for keys and mappings that are not known.
This means, that the original value is just passed instead of being mapped. There is just a warning in the log files.

This fail strategy can be configured as part of the builder. There is another fail strategy implemented (`FAIL_WITH_EXCEPTION`),
that throws an `IllegalArgumentException` when no mapping can be found with no log message.

## Usage
```
    implementation 'org.sdase.commons:sda-commons-server-key-mgmt:<version>'
```

Initialization of bundle:
```java    
  private final KeyMgmtBundle<KeyMgmtBundleTestConfig> keyMgmt =
        KeyMgmtBundle.builder()
            .withKeyMgmtConfigProvider(KeyMgmtBundleTestConfig::getKeyMgmt)
            .withFailStrategy(FAIL_WITH_EXCEPTION)
            .build();
```

The [configuration](src/main/java/org/sdase/commons/keymgmt/config/KeyMgmtConfig.java) includes the paths to the mapping and keys yaml files as well as the option to disable value validation.
The following listing shows a yaml snippet for the `keyMgmt` configuration. 
```yaml
keyMgmt:
  apiKeysDefinitionPath: "/keys"     # path to key definition yaml files
  mappingDefinitionPath: "/mappings" # path to mapping definition yaml files
  disableValidation: false           # option to deactivate input value validation for keys. Default: false
```

### @PlatformKey
The annotation `@PlatformKey` will trigger a validator to verify if the received value is a valid key for the referenced platform key.
```java
@PlatformKey("GENDER")
private String genderKey
```

### Key yaml file
The yaml file or keys may contain one or more [KeyDefinition](src/main/java/org/sdase/commons/keymgmt/model/KeyDefinition.java) documents.

__Example:__
```yaml
name: GENDER
desciption: "Gender of a human"
values:
  - value: MALE
    description: "male gender"
  - value: FEMALE
    description: "female gender"
  - value: Other
    description: "other if male or female does not fit"
```

### Mapping yaml file
The yaml file for mappings may contain one or more [KeyMappingModel](src/main/java/org/sdase/commons/keymgmt/model/KeyMappingModel.java) documents.

__Example:__
```yaml
name: GENDER
mapping:
  apiToImplBidirectional:
    - api: "MALE"
      impl: "m"
    - api: "FEMALE"
      impl: "F"
    - api: "OTHER"
      impl: "d"
---
name: SALUTATION
mapping:
  apiToImplBidirectional:
    - api: "MRS"
      impl: "1"
    - api: "MR"
      impl: "0"
  implToApi:
    - impl: "2"
      api: "MRS"
```
