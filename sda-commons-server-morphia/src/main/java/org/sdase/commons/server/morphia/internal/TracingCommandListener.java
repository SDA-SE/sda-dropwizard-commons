package org.sdase.commons.server.morphia.internal;

import com.mongodb.event.CommandFailedEvent;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import com.mongodb.event.CommandSucceededEvent;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.mongo.v3_1.MongoTelemetry;

public class TracingCommandListener implements CommandListener {
  private final CommandListener delegate;

  public TracingCommandListener(OpenTelemetry openTelemetry) {
    delegate = MongoTelemetry.builder(openTelemetry).build().newCommandListener();
  }

  @Override
  public void commandStarted(CommandStartedEvent event) {
    delegate.commandStarted(event);
  }

  @Override
  public void commandSucceeded(CommandSucceededEvent event) {
    delegate.commandSucceeded(event);
  }

  @Override
  public void commandFailed(CommandFailedEvent event) {
    delegate.commandFailed(event);
  }
}
