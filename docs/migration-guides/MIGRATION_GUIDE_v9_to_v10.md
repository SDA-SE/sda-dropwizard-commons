# Migration Guide from v9 to v10

## AsyncAPI module removed

AsyncAPI generation, formally provided by `sda-commons-shared-asyncapi`, is now integrated in
sda-commons-server-testing.

For migration imports need to be changed:

| Old package (`sda-commons-shared-asyncapi`)             | New package (`sda-commons-server-testing`)             |
|---------------------------------------------------------|--------------------------------------------------------|
| `org.sdase.commons.shared.asyncapi.jsonschema.victools` | `org.sdase.commons.server.testing.jsonschema.victools` |
| `org.sdase.commons.shared.asyncapi.jsonschema`          | `org.sdase.commons.server.testing.jsonschema`          |
| `org.sdase.commons.shared.asyncapi.util`                | `org.sdase.commons.server.testing.json`                |
| `org.sdase.commons.shared.asyncapi`                     | `org.sdase.commons.server.testing.asyncapi`            |
