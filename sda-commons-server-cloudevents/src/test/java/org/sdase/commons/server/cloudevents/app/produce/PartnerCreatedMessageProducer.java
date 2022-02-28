package org.sdase.commons.server.cloudevents.app.produce;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.cloudevents.CloudEvent;
import org.sdase.commons.server.cloudevents.CloudEventsProducerHelper;
import org.sdase.commons.server.cloudevents.app.Partner;
import org.sdase.commons.server.kafka.producer.MessageProducer;

public class PartnerCreatedMessageProducer {

  private final MessageProducer<String, CloudEvent> producer;

  private final CloudEventsProducerHelper<PartnerCreatedEvent> cloudEventsProducerHelper;

  public PartnerCreatedMessageProducer(
      MessageProducer<String, CloudEvent> producer,
      CloudEventsProducerHelper<PartnerCreatedEvent> cloudEventsProducerHelper) {
    this.producer = producer;
    this.cloudEventsProducerHelper = cloudEventsProducerHelper;
  }

  public void produce(Partner partner) throws JsonProcessingException {
    PartnerCreatedEvent partnerCreatedEvent = new PartnerCreatedEvent().setId(partner.getId());
    producer.send(
        partner.getId(), cloudEventsProducerHelper.wrap(partnerCreatedEvent, partner.getId()));
  }
}
