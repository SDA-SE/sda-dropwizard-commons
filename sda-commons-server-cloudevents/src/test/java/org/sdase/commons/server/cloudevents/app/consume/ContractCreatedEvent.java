package org.sdase.commons.server.cloudevents.app.consume;

import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import org.sdase.commons.server.cloudevents.CloudEventV1;

public class ContractCreatedEvent extends CloudEventV1<ContractCreatedEvent.ContractCreated> {

  @Schema(
      defaultValue =
          "/SDA-SE/insurance-contract/insurance-contract-stack/insurance-contract-service")
  @Override
  public URI getSource() {
    return super.getSource();
  }

  @Schema(defaultValue = "com.sdase.contract.foo.contract.created")
  @Override
  public String getType() {
    return super.getType();
  }

  public static class ContractCreated {

    private String contractId;

    private String partnerId;

    public String getContractId() {
      return contractId;
    }

    public ContractCreated setContractId(String contractId) {
      this.contractId = contractId;
      return this;
    }

    public String getPartnerId() {
      return partnerId;
    }

    public ContractCreated setPartnerId(String partnerId) {
      this.partnerId = partnerId;
      return this;
    }
  }
}
