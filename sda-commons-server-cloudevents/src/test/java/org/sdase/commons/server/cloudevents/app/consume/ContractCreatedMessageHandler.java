package org.sdase.commons.server.cloudevents.app.consume;

import io.cloudevents.CloudEvent;
import java.io.IOException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.sdase.commons.server.cloudevents.CloudEventsConsumerHelper;
import org.sdase.commons.server.kafka.consumer.ErrorHandler;
import org.sdase.commons.server.kafka.consumer.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContractCreatedMessageHandler
    implements MessageHandler<String, CloudEvent>, ErrorHandler<String, CloudEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ContractCreatedMessageHandler.class);

  private final CloudEventsConsumerHelper ceConsumer;

  private final InMemoryStore inMemoryStore;

  public ContractCreatedMessageHandler(
      CloudEventsConsumerHelper ceConsumer, InMemoryStore inMemoryStore) {
    this.ceConsumer = ceConsumer;
    this.inMemoryStore = inMemoryStore;
  }

  @Override
  public void handle(ConsumerRecord<String, CloudEvent> record) {
    try {
      ContractCreatedEvent event = ceConsumer.unwrap(record.value(), ContractCreatedEvent.class);
      inMemoryStore.addContractCreatedEvent(event);
      LOGGER.info(
          "Contract {} created for partner {}", event.getContractId(), event.getPartnerId());
    } catch (IOException e) {
      LOGGER.error("Can't read CloudEvent's data", e);
    }
  }

  @Override
  public boolean handleError(
      ConsumerRecord<String, CloudEvent> record,
      RuntimeException e,
      Consumer<String, CloudEvent> consumer) {
    return false;
  }
}
