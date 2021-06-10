package org.sdase.commons.keymgmt.config;

import javax.validation.constraints.NotNull;

@SuppressWarnings("unused")
public class KeyMgmtConfig {

  /** path to the yaml file with keys and values definitions for that keys */
  @NotNull private String apiKeysDefinitionPath;

  /** path to the yaml file with mappings for the defined keys */
  private String mappingDefinitionPath;

  /** allows to disable the validation for key values in incoming messages */
  private boolean disableValidation = false;

  public String getApiKeysDefinitionPath() {
    return apiKeysDefinitionPath;
  }

  public KeyMgmtConfig setApiKeysDefinitionPath(String apiKeysDefinitionPath) {
    this.apiKeysDefinitionPath = apiKeysDefinitionPath;
    return this;
  }

  public String getMappingDefinitionPath() {
    return mappingDefinitionPath;
  }

  public KeyMgmtConfig setMappingDefinitionPath(String mappingDefinitionPath) {
    this.mappingDefinitionPath = mappingDefinitionPath;
    return this;
  }

  public boolean isDisableValidation() {
    return disableValidation;
  }

  public KeyMgmtConfig setDisableValidation(boolean disableValidation) {
    this.disableValidation = disableValidation;
    return this;
  }
}
