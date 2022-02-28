package org.sdase.commons.server.cloudevents.app.consume;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.sdase.commons.server.kafka.consumer.ErrorHandler;
import org.sdase.commons.server.kafka.consumer.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContractCreatedPlainMessageHandler
    implements MessageHandler<String, ContractCreatedEvent>,
        ErrorHandler<String, ContractCreatedEvent> {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ContractCreatedPlainMessageHandler.class);

  private final InMemoryStore inMemoryStore;

  public ContractCreatedPlainMessageHandler(InMemoryStore inMemoryStore) {
    this.inMemoryStore = inMemoryStore;
  }

  @Override
  public void handle(ConsumerRecord<String, ContractCreatedEvent> record) {
    ContractCreatedEvent event = record.value();
    inMemoryStore.addContractCreatedEvent(event);
    LOGGER.info("Contract {} created for partner {}", event.getContractId(), event.getPartnerId());
  }

  @Override
  public boolean handleError(
      ConsumerRecord<String, ContractCreatedEvent> record,
      RuntimeException e,
      Consumer<String, ContractCreatedEvent> consumer) {
    return false;
  }
}
