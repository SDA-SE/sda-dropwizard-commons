package org.sdase.commons.server.cloudevents.app.produce;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDefault;
import java.net.URI;
import org.sdase.commons.server.cloudevents.CloudEventV1;

public class PartnerCreatedEvent extends CloudEventV1<PartnerCreatedEvent.PartnerCreated> {

  @JsonSchemaDefault("`/SDA-SE/partner/partner-stack/partner-service`")
  @Override
  public URI getSource() {
    return super.getSource();
  }

  @JsonPropertyDescription("`PARTNER_CREATED`")
  @JsonSchemaDefault("PARTNER_CREATED")
  @Override
  public String getType() {
    return super.getType();
  }

  public static class PartnerCreated {
    private String id;

    public String getId() {
      return id;
    }

    public PartnerCreated setId(String id) {
      this.id = id;
      return this;
    }
  }
}
