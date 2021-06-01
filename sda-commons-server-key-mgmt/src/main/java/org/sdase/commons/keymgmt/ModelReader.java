package org.sdase.commons.keymgmt;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;
import org.sdase.commons.keymgmt.model.KeyDefinition;
import org.sdase.commons.keymgmt.model.KeyMappingModel;
import org.sdase.commons.shared.yaml.YamlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelReader {

  private static final Logger LOG = LoggerFactory.getLogger(ModelReader.class);

  private ModelReader() {
    // prevent instance
  }

  static Map<String, KeyMappingModel> parseMappingFile(String mappingDefinitionPath)
      throws IOException {

    if (mappingDefinitionPath == null) {
      LOG.warn("mapping definition file is 'null'. Check your configuration.");
      return Collections.emptyMap();
    }

    Map<String, KeyMappingModel> mappings = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    try (Stream<Path> stream = Files.list(Paths.get(mappingDefinitionPath))) {
      stream.forEach(
          file -> {
            try (FileInputStream fileInputStream = new FileInputStream(file.toFile())) {
              List<KeyMappingModel> keyMappingModels =
                  YamlUtil.loadList(fileInputStream, new TypeReference<KeyMappingModel>() {});
              keyMappingModels.forEach(mm -> mappings.put(mm.getName(), mm));
            } catch (IOException e) {
              LOG.warn("cannot load key definition for file '{}'", file);
            }
          });
      return mappings;
    } catch (NoSuchFileException e) {
      LOG.error(
          "Mapping definition path '{}' does not exist. Check your configuration",
          mappingDefinitionPath);
      throw e;
    }
  }

  static Map<String, KeyDefinition> parseApiKeys(String apiKeysDefinitionPath) throws IOException {
    if (apiKeysDefinitionPath == null) {
      LOG.warn("key definition file is 'null'. Check your configuration.");
      return Collections.emptyMap();
    }

    Map<String, KeyDefinition> keys = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    try (Stream<Path> stream = Files.list(Paths.get(apiKeysDefinitionPath))) {
      stream.forEach(
          file -> {
            try (FileInputStream fileInputStream = new FileInputStream(file.toFile())) {
              List<KeyDefinition> defs = YamlUtil.loadList(fileInputStream, KeyDefinition.class);
              defs.forEach(def -> keys.put(def.getName(), def));
            } catch (IOException e) {
              LOG.warn("cannot load key definition for file '{}'", file);
            }
          });
      return keys;
    } catch (NoSuchFileException e) {
      LOG.error(
          "Key definition path '{}' does not exist. Check your configuration",
          apiKeysDefinitionPath);
      throw e;
    }
  }
}
