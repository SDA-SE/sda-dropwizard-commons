package org.sdase.commons.server.cloudevents.app.consume;

import java.util.ArrayList;
import java.util.List;

public class InMemoryStore {

  private List<ContractCreatedEvent> contractCreatedEvents = new ArrayList<>();

  public void addContractCreatedEvent(ContractCreatedEvent event) {
    contractCreatedEvents.add(event);
  }

  public List<ContractCreatedEvent> getContractCreatedEvents() {
    return contractCreatedEvents;
  }

  public void clear() {
    contractCreatedEvents.clear();
  }
}
