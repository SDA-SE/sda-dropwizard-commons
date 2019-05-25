package org.sdase.commons.server.morphia.internal;

import com.mongodb.event.CommandFailedEvent;
import com.mongodb.event.CommandStartedEvent;
import com.mongodb.event.CommandSucceededEvent;
import io.opentracing.Span;
import io.opentracing.contrib.mongo.common.SpanDecorator;
import io.opentracing.tag.Tags;

public class NoStatementSpanDecorator implements SpanDecorator {

  @Override
  public void commandStarted(CommandStartedEvent event, Span span) {
    // Replace with a copy of the command that contains no personal data
    Tags.DB_STATEMENT.set(span, CommandSanitizer.sanitize(event.getCommand()).toString());
  }

  @Override
  public void commandSucceeded(CommandSucceededEvent event, Span span) {
    // no additional span data here
  }

  @Override
  public void commandFailed(CommandFailedEvent event, Span span) {
    // no additional span data here
  }
}
