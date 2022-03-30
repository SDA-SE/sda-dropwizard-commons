package org.sdase.commons.server.cloudevents.app.consume;

import java.util.ArrayList;
import java.util.List;

public class InMemoryStore {

  private final List<ContractCreatedEvent.ContractCreated> contractCreatedEvents =
      new ArrayList<>();

  public void addContractCreatedEvent(ContractCreatedEvent.ContractCreated event) {
    contractCreatedEvents.add(event);
  }

  public List<ContractCreatedEvent.ContractCreated> getContractCreatedEvents() {
    return contractCreatedEvents;
  }

  public void clear() {
    contractCreatedEvents.clear();
  }
}
