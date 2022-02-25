/**
 * Copyright 2017-2018 The OpenTracing Authors
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Based on https://github.com/opentracing-contrib/java-spring-cloud/blob/1be2ef50a2ca930319cda4db06088009080d94b7/instrument-starters/opentracing-spring-cloud-core/src/main/java/io/opentracing/contrib/spring/cloud/log/SpanLogsAppender.java
 */
package org.sdase.commons.server.opentracing.logging;

import static io.opentracing.log.Fields.ERROR_KIND;
import static io.opentracing.log.Fields.ERROR_OBJECT;
import static io.opentracing.log.Fields.EVENT;
import static io.opentracing.log.Fields.MESSAGE;
import static io.opentracing.log.Fields.STACK;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Pavol Loffay
 */
public class SpanLogsAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

  private final Tracer tracer;

  public SpanLogsAppender(Tracer tracer) {
    this.name = SpanLogsAppender.class.getSimpleName();
    this.tracer = tracer;
  }

  /**
   * This is called only for configured levels. It will not be executed for DEBUG level if root
   * logger is INFO.
   */
  @Override
  protected void append(ILoggingEvent event) {
    Span span = tracer.activeSpan();
    if (span != null) {
      Map<String, Object> logs = new HashMap<>(6);
      logs.put("logger", event.getLoggerName());
      logs.put("level", event.getLevel().toString());
      logs.put("thread", event.getThreadName());
      logs.put(MESSAGE, event.getFormattedMessage());

      if (event.getLevel().isGreaterOrEqual(Level.ERROR)) {
        logs.put(EVENT, Tags.ERROR.getKey());
      }

      IThrowableProxy throwableProxy = event.getThrowableProxy();
      if (throwableProxy instanceof ThrowableProxy) {
        Throwable throwable = ((ThrowableProxy) throwableProxy).getThrowable();
        String stackTrace = ThrowableProxyUtil.asString(throwableProxy);

        logs.put(STACK, stackTrace);

        if (throwable != null) {
          logs.put(ERROR_OBJECT, throwable);
          logs.put(ERROR_KIND, throwable.getClass().getName());
        }
      }
      span.log(TimeUnit.MICROSECONDS.convert(event.getTimeStamp(), TimeUnit.MILLISECONDS), logs);
    }
  }
}
