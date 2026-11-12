# Migration Guide from v9 to v10

## Golden-file YAML assertions use Jackson 3

`GoldenFileAssertions.hasYamlContentAndUpdateGolden` now uses Jackson 3's `YAMLMapper`
instead of Dropwizard's Jackson 2 mapper. It explicitly uses the YAML 1.2 core schema.
No Dropwizard POJO modules are needed because the assertion compares parsed trees, not Java objects.

Check golden files for these scalar parsing differences:

| YAML input                        | Previous behavior                       | New behavior                               |
|-----------------------------------|-----------------------------------------|--------------------------------------------|
| Unquoted `yes`, `no`, `on`, `off` | Parsed as booleans                      | Parsed as strings                          |
| `value: .nan`                     | Parsing failed with the previous mapper | Accepted as a special floating-point value |

Use `true` and `false` for booleans. Quote values intended as strings. For example,
`value: yes` no longer compares equal to `value: true`, but does compare equal to `value: "yes"`.

Jackson 3 enables trailing-token checks (`FAIL_ON_TRAILING_TOKENS`) by default. Both the golden file
and the expected content must contain a single YAML document. Previously, later documents were
silently ignored, so the assertion could pass despite differences in those documents. Extra
documents
now cause a parsing exception, even when their content matches.

Split multi-document YAML into separate golden files and assert each document separately.
The existing update policy is unchanged: local runs write the complete expected text even when
parsing fails; CI runs never update the golden file. Updating a file alone does not resolve a
multi-document parsing failure.

## AsyncAPI generation uses Jackson 3

AsyncAPI generation in `sda-commons-server-testing` now uses Jackson 3 for YAML parsing,
JSON tree processing, and YAML output.

Update code that consumes `FinalBuilder.generate()` or implements a custom `JsonSchemaBuilder`
to use `tools.jackson.databind.JsonNode` instead of `com.fasterxml.jackson.databind.JsonNode`.
Custom schema builders must return Jackson 3 nodes, including the nodes in their result maps.
Jackson 2 and Jackson 3 node types are not interchangeable.

```java
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
```

`generateYaml()` still returns a `String`. Review regenerated AsyncAPI golden files when upgrading.
Base templates are parsed with Jackson 3's `YAMLMapper`, so the scalar parsing differences described
above also apply. Templates must contain a single YAML document; trailing documents are rejected.
YAML parsing and serialization failures are wrapped in `YamlGenerationException`.

## JSON utilities distinguish Jackson 2 and Jackson 3

The utilities in `org.sdase.commons.server.testing.json` now distinguish the Jackson major version
in their node types:

| Operation                          | Jackson 3 (`tools.jackson.databind`) | Jackson 2 (`com.fasterxml.jackson.databind`) |
|------------------------------------|--------------------------------------|----------------------------------------------|
| Sort object fields in place        | `JsonNodeUtil`                       | `JsonJackson2NodeUtil`                       |
| Rewrite or merge `$ref` references | `RefUtil`                            | `RefJackson2Util`                            |

`JsonNodeUtil` and `RefUtil` now accept Jackson 3 nodes. Update their callers and reference-rewriter
callbacks to use Jackson 3 types, including `StringNode` instead of Jackson 2's `TextNode`.

If you still need Jackson 2 nodes, use `JsonJackson2NodeUtil` or `RefJackson2Util` as a temporary
migration path. Both are deprecated since 10.0.0. They do not convert between Jackson versions.
Migrate the nodes and their callers to Jackson 3 before switching to `JsonNodeUtil` or `RefUtil`.

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
