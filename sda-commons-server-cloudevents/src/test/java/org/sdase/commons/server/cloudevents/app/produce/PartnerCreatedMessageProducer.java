package org.sdase.commons.server.cloudevents.app.produce;

import java.net.URI;
import org.sdase.commons.server.cloudevents.app.Partner;
import org.sdase.commons.server.kafka.producer.MessageProducer;

public class PartnerCreatedMessageProducer {

  private final MessageProducer<String, PartnerCreatedEvent> producer;

  public PartnerCreatedMessageProducer(MessageProducer<String, PartnerCreatedEvent> producer) {
    this.producer = producer;
  }

  public void produce(Partner partner) {
    PartnerCreatedEvent cloudEvent =
        (PartnerCreatedEvent)
            new PartnerCreatedEvent()
                .setData(new PartnerCreatedEvent.PartnerCreated().setId(partner.getId()))
                .setSource(URI.create("/SDA-SE/partner/partner-example/partner-example-service"))
                .setSubject(partner.getId())
                .setType("PARTNER_CREATED");

    producer.send(partner.getId(), cloudEvent);
  }
}
