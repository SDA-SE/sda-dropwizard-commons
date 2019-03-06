# SDA Commons Shared YAML

This module contains a class [`YamlUtil`](./src/main/java/org/sdase/commons/shared/yaml/YamlUtil.java)
providing static methods for YAML-file handling.


## Usage

The [`YamlUtil`](./src/main/java/org/sdase/commons/shared/yaml/YamlUtil.java)
provides an overloaded method for loading YAML-files and one for serialization.

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
