package org.sdase.commons.server.swagger;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.LinkedHashMap;
import org.sdase.commons.shared.yaml.YamlUtil;

public class SwaggerFileHelper {
  private SwaggerFileHelper() {
    // prevent initialization
  }

  /**
   * Normalize an textual OpenApi in the {@code yaml} format and remove all environment-specific
   * contents.
   *
   * <ul>
   *   <li>Removes the {@code host} field that is automatically generated but disrupts the
   *       repeatability in tests where the application run on random ports.
   * </ul>
   *
   * @param yaml the content of the {@code swagger.yaml} file
   * @return the cleaned up {@code yaml}
   */
  public static String normalizeSwaggerYaml(String yaml) {
    LinkedHashMap<String, Object> yamlMap =
        YamlUtil.load(yaml, new TypeReference<LinkedHashMap<String, Object>>() {});
    yamlMap.remove("host");
    return YamlUtil.writeValueAsString(yamlMap);
  }
}
