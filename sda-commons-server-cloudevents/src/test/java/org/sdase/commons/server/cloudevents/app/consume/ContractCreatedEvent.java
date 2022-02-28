package org.sdase.commons.server.cloudevents.app.consume;

public class ContractCreatedEvent {

  private String contractId;

  private String partnerId;

  public String getContractId() {
    return contractId;
  }

  public ContractCreatedEvent setContractId(String contractId) {
    this.contractId = contractId;
    return this;
  }

  public String getPartnerId() {
    return partnerId;
  }

  public ContractCreatedEvent setPartnerId(String partnerId) {
    this.partnerId = partnerId;
    return this;
  }
}
