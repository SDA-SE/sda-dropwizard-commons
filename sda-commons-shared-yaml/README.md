# SDA Commons Shared YAML

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-shared-yaml/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-shared-yaml)

This module contains a class [`YamlUtil`](./src/main/java/org/sdase/commons/shared/yaml/YamlUtil.java)
providing static methods for YAML-file handling.


## Usage

The [`YamlUtil`](./src/main/java/org/sdase/commons/shared/yaml/YamlUtil.java)
provides an overloaded method for loading YAML-files and one for serialization.

### A single object

Given the following YAML-file

```yaml
message:  "Hello"
attribute: "attribute1"
```

it is possible to load and serialize a single object:

```java
import org.sdase.commons.shared.yaml.YamlUtil;

class MyClass {
  
  public static void main(final String[] args) {
    InputStream resource = this.getClass().getClassLoader().getResourceAsStream("sample.yml");
    TestBean tb = YamlUtil.load(resource, TestBean.class);
    
    // ...
    
    String serializedClass = YamlUtil.writeValueAsString(tb);
  }
}
```

### List of objects

To load a list of objects from a YAML-file like

```yaml
- message: Hello World!
  attribute: Foo
  id: 123
- message: Hello Universe!
  attribute: Bar
  id: 456
```

use a `TypeReference<T>` as the second parameter:

```java
List<TestBean> beans = YamlUtil.load(resource, new TypeReference<List<TestBean>>() {});
```

### Multiple documents

To load a YAML-file that contains multiple YAML-documents, like

```yaml 
message: Hello World!
attribute: Foo
id: 123
---
message: Hello Universe!
attribute: Bar
id: 456
```

use `YamlUtil.loadList()`:

```java
List<TestBean> beans = YamlUtil.loadList(resource, TestBean.class);
```
