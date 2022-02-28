package org.sdase.commons.server.cloudevents.app.produce;

public class PartnerCreatedEvent {

  private String id;

  public String getId() {
    return id;
  }

  public PartnerCreatedEvent setId(String id) {
    this.id = id;
    return this;
  }
}
