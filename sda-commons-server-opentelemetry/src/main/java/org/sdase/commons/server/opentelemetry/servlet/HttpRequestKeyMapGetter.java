package org.sdase.commons.server.opentelemetry.servlet;

import io.opentelemetry.context.propagation.TextMapGetter;
import java.util.Collections;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

public class HttpRequestKeyMapGetter implements TextMapGetter<HttpServletRequest> {
  @Override
  public Iterable<String> keys(HttpServletRequest carrier) {
    return Collections.list(carrier.getHeaderNames());
  }

  @Nullable
  @Override
  public String get(@Nullable HttpServletRequest carrier, String key) {
    if (carrier == null) {
      throw new AssertionError();
    }
    return carrier.getHeader(key);
  }
}
