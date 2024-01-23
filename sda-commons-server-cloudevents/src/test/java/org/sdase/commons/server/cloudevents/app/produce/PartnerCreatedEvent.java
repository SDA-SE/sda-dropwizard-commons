package org.sdase.commons.server.cloudevents.app.produce;

import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import org.sdase.commons.server.cloudevents.CloudEventV1;

public class PartnerCreatedEvent extends CloudEventV1<PartnerCreatedEvent.PartnerCreated> {

  @Schema(defaultValue = "`/SDA-SE/partner/partner-stack/partner-service`")
  @Override
  public URI getSource() {
    return super.getSource();
  }

  @Schema(
      description = "`com.sdase.partner.ods.partner.created`",
      defaultValue = "com.sdase.partner.ods.partner.created")
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
