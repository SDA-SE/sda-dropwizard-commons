package org.sdase.commons.keymgmt.model;

import java.util.*;

@SuppressWarnings("unused")
public class ValueMappingModel {

  private final Map<String, String> apiToImplBidirectional = new HashMap<>();
  private final Map<String, String> implToApiBidirectional = new HashMap<>();

  // maps between impl and api with higher precedence than the bidirectional mapping
  private final Map<String, String> implToApi = new HashMap<>();
  private final Map<String, String> apiToImpl = new HashMap<>();

  public ValueMappingModel setApiToImplBidirectional(List<ValueMapping> apiToImplBidirectional) {
    this.apiToImplBidirectional.clear();
    this.implToApiBidirectional.clear();
    apiToImplBidirectional.forEach(
        vm -> {
          this.apiToImplBidirectional.put(vm.getApi(), vm.getImpl());
          this.implToApiBidirectional.put(vm.getImpl(), vm.getApi());
        });
    return this;
  }

  public ValueMappingModel setImplToApi(List<ValueMapping> implToApi) {
    this.implToApi.clear();
    implToApi.forEach(vm -> this.implToApi.put(vm.getImpl(), vm.getApi()));
    return this;
  }

  public ValueMappingModel setApiToImpl(List<ValueMapping> apiToImpl) {
    this.apiToImpl.clear();
    apiToImpl.forEach(vm -> this.apiToImpl.put(vm.getApi(), vm.getImpl()));
    return this;
  }

  public Optional<String> mapToImpl(String apiValue) {
    return Optional.ofNullable(
        apiToImpl.getOrDefault(
            apiValue.toUpperCase(Locale.ROOT),
            apiToImplBidirectional.get(apiValue.toUpperCase(Locale.ROOT))));
  }

  public Optional<String> mapToApi(String implValue) {
    return Optional.ofNullable(
        implToApi.getOrDefault(implValue, implToApiBidirectional.get(implValue)));
  }
}
